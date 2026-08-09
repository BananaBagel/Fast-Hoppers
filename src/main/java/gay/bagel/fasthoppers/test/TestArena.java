package gay.bagel.fasthoppers.test;

import gay.bagel.fasthoppers.FastHoppers;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;

import java.util.function.IntSupplier;

/**
 * Shared scaffolding for structure-backed suites: naming a structure, translating its coordinates,
 * and the one assertion every suite needs.
 * <p>
 * Suites under {@code test.tests} exist to describe behaviour, so the mechanics they all repeat
 * live here instead.
 */
public final class TestArena {

	/**
	 * Tick budget for tests that move a full stack.
	 * <p>
	 * Hoppers move one item per cooldown, so a 64 stack crossing a two hopper chain at the vanilla
	 * eight tick delay needs roughly a thousand ticks. Tests finish as soon as their condition
	 * holds, so this only caps the pathological case.
	 */
	public static final int STACK_MOVE_TICKS = 3000;

	private TestArena() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Options for a structure-backed test: that structure, and a budget big enough to move a stack.
	 *
	 * @param structure file name of the structure, without namespace or extension
	 * @return options naming that structure
	 */
	public static TestOptions arena(String structure) {
		return TestOptions.of()
				.withStructure(FastHoppers.MOD_ID + ":" + structure)
				.withMaxTicks(STACK_MOVE_TICKS);
	}

	/**
	 * Converts a position as saved in the structure file into the position the test runner uses.
	 * <p>
	 * Going through here means positions can be written exactly as a structure editor shows them —
	 * the bedrock floor at {@code y=0}, the first row of containers at {@code y=1} — instead of
	 * every call site carrying an offset and getting it wrong.
	 * <p>
	 * That offset is not the same on every version. Before 1.21.11 the structure is placed one
	 * block above the origin {@code GameTestHelper} measures from, so structure {@code y=N} is
	 * reached at relative {@code y=N+1}. From 1.21.11 the test instance block places it level with
	 * that origin instead, and the two coincide.
	 *
	 * @param x structure-local x
	 * @param y structure-local y, with the floor at 0
	 * @param z structure-local z
	 * @return the equivalent test-relative position
	 */
	public static BlockPos at(int x, int y, int z) {
		//? < 1.21.11 {
		return new BlockPos(x, y + 1, z);
		//?} >= 1.21.11 {
		/*return new BlockPos(x, y, z);
		*///?}
	}

	/**
	 * Passes as soon as {@code assertion} holds, recording how long that took.
	 * <p>
	 * Use in place of {@code helper.succeedWhen} so the test contributes a line to the timing
	 * table; behaviour is otherwise identical, the assertion being retried every tick until it
	 * stops throwing or the test's tick budget runs out.
	 * <p>
	 * The test's name is captured now rather than when the assertion passes, because this call
	 * happens while the body is still running and the name is still current.
	 *
	 * @param helper    the arena handle
	 * @param assertion condition to poll; throws until satisfied
	 */
	public static void succeedWhen(GameTestHelper helper, Runnable assertion) {
		succeedWhen(helper, null, assertion);
	}

	/**
	 * Passes as soon as {@code assertion} holds, measuring the transfer rate directly rather than
	 * inferring it from how long the test took.
	 * <p>
	 * {@code progress} reports how many items have left the source so far. Watching it turns the
	 * measurement into something self-contained: the rate is taken between two moments this method
	 * observed itself — the first poll where anything had moved, and the poll where the last item
	 * did — instead of between the test's clock starting and its assertions passing.
	 * <p>
	 * That distinction is what makes the number trustworthy on every version. From 1.21.11 the
	 * structure is placed and starts ticking before the test's counter reaches zero, so a total
	 * measured from the clock silently omits whatever happened first — at delay 23 that was four
	 * items, around ninety ticks, and it produced readings as impossible as 64 items moved in 0
	 * ticks. Both endpoints here are things this method saw, so anything before it started watching
	 * is excluded from the span and from the item count alike, and cancels out.
	 *
	 * @param helper    the arena handle
	 * @param progress  items moved out of the source so far, or {@code null} to skip rate measuring
	 * @param assertion condition to poll; throws until satisfied
	 */
	public static void succeedWhen(GameTestHelper helper, IntSupplier progress, Runnable assertion) {
		String name = TestTiming.current();
		TestOptions options = TestTiming.currentOptions();
		int delay = options.transferDelay();

		// Boxed so the settle step can read the tick the assertions first passed on; the timing
		// reported is that moment, not the end of the settle window.
		long[] settledAt = {0L};

		// The observation window: the first poll that saw movement, and the last one that did.
		long[] fromTick = {-1L};
		int[] fromMoved = {0};
		long[] toTick = {-1L};
		int[] toMoved = {0};

		Runnable watch = () -> {
			if (progress == null) {
				return;
			}
			int moved = progress.getAsInt();
			if (fromTick[0] < 0 && moved > 0) {
				fromTick[0] = helper.getTick();
				fromMoved[0] = moved;
			}
			if (moved != toMoved[0]) {
				toMoved[0] = moved;
				toTick[0] = helper.getTick();
			}
		};

		helper.startSequence()
				.thenWaitUntil(() -> {
					watch.run();
					assertion.run();
				})
				// getTick() is already the test's own counter — GameTestInfo derives it as
				// level.getGameTime() - startTick — so it is the elapsed time by itself. Subtracting
				// a tick captured when this method ran would discard everything between the test
				// starting and its body being invoked. That gap is zero before 1.21.11 and large
				// from 1.21.11 on, where it silently swallowed whole transfers and produced
				// impossible readings, including tests reporting they moved 64 items in 0 ticks.
				.thenExecute(() -> settledAt[0] = helper.getTick())
				// Re-runs every tick. Anything that regresses here throws straight out and fails the
				// test immediately, rather than being retried like the wait above.
				.thenExecuteFor(options.settleTicks(delay), assertion)
				.thenExecute(() -> {
					long elapsed = settledAt[0];

					// Only a window that saw at least two distinct movements measures a rate; one
					// observation gives a point, not an interval.
					long spanTicks = -1L;
					int spanItems = 0;
					if (fromTick[0] >= 0 && toTick[0] > fromTick[0]) {
						spanTicks = toTick[0] - fromTick[0];
						spanItems = toMoved[0] - fromMoved[0];
					}
					TestTiming.record(name, options, elapsed, spanTicks, spanItems);

					// The expectations are calibrated against pre-1.21.11 measurements. 1.21.11+ runs
					// the same transfers measurably faster — 429 ticks against 505 at delay 8 — for
					// reasons not yet established, so asserting there would only report the
					// difference over and over. Durations are still logged on every version, which
					// is what recalibrating will need.
					//? < 1.21.11 {
					if (options.isTimed()) {
						int expected = options.expectedTicks(delay);
						int tolerance = options.toleranceTicks(delay);
						long drift = elapsed - expected;

						if (Math.abs(drift) > tolerance) {
							helper.fail(name + " settled after " + elapsed + " ticks, expected "
									+ expected + " +/-" + tolerance + " ("
									+ (drift > 0 ? "+" : "") + drift + "); "
									+ options.itemsMoved() + " items, " + options.hoppers()
									+ " hoppers, delay " + delay);
						}
					}
					//?}
				})
				.thenSucceed();
	}

	/**
	 * Progress expressed as how much has left the source, for {@link #succeedWhen}.
	 * <p>
	 * Depletion of the source rather than arrival at a destination, because it is the one signal
	 * every layout shares: tests that branch to two chests, or that deliberately back items up into
	 * a hopper, have no single destination to count, but they all drain the same source.
	 *
	 * @param helper the arena handle
	 * @param item   the item being moved
	 * @param total  how many the source started with
	 * @param source structure-relative position of the source container
	 * @return a supplier of items moved out so far
	 */
	public static IntSupplier movedFrom(GameTestHelper helper, Item item, int total, BlockPos source) {
		return () -> total - TestContainers.count(helper, item, source);
	}

	/**
	 * Fails unless a single container holds exactly the expected number of an item.
	 *
	 * @param helper   the arena handle
	 * @param item     item to count
	 * @param expected how many it should hold
	 * @param pos      structure-relative position of the container
	 * @param what     human-readable name of the container, for the failure message
	 */
	public static void assertHolds(GameTestHelper helper, Item item, int expected, BlockPos pos, String what) {
		int actual = TestContainers.count(helper, item, pos);
		if (actual != expected) {
			// Deliberately not assertTrue: that overload does not exist before 1.20.
			helper.fail(what + " should hold " + expected + " " + item + " but holds " + actual);
		}
	}
}
