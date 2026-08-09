package gay.bagel.fasthoppers.test;

import gay.bagel.fasthoppers.FastHoppers;

import net.minecraft.world.level.block.Rotation;

/**
 * Per-test settings, separated from the test body so the same declaration serves every version.
 * <p>
 * Which consumer reads which field depends on the era. Before 1.21.11 all of these go straight
 * into a {@code TestFunction} at runtime. From 1.21.11 the runtime only needs the body — the rest
 * describes the {@code test_instance} datapack entry instead, and is read when generating it.
 * Keeping the settings here rather than at either call site is what lets one declaration drive
 * both.
 * <p>
 * Suites do not normally set {@link #batch} or {@link #transferDelay}; those are filled in when
 * {@code FastHoppersGameTests} fans each test out across the transfer delays under test.
 * <p>
 * Instances are immutable; build them by chaining {@code with*} calls onto {@link #of()}.
 *
 * @param structure     namespaced id of the structure template the test runs inside
 * @param maxTicks      how long the test may run before the runner fails it
 * @param setupTicks    delay after the structure is placed before the body starts
 * @param required      whether a failure fails the whole run, as opposed to being reported only
 * @param rotation      rotation applied to the structure when it is placed
 * @param batch         group this test runs in; tests sharing a batch run at the same time
 * @param transferDelay hopper transfer delay this test runs under, or {@link #NO_TRANSFER_DELAY}
 * @param itemsMoved    items the test waits to see moved, or 0 if it is not timed
 * @param hoppers       hoppers in the structure's chain, or 0 if the test is not timed
 */
public record TestOptions(
		String structure,
		int maxTicks,
		long setupTicks,
		boolean required,
		Rotation rotation,
		String batch,
		int transferDelay,
		int itemsMoved,
		int hoppers
) {

	/**
	 * The shared empty arena: a 3x9x3 box of air, backed by
	 * {@code data/fasthoppers/structure/empty_3x9x3.nbt}.
	 */
	public static final String EMPTY_STRUCTURE = FastHoppers.MOD_ID + ":empty_3x9x3";

	/** Vanilla's batch name, used when a test has not been assigned one. */
	public static final String DEFAULT_BATCH = "defaultBatch";

	/** {@link #transferDelay} value meaning "leave the gamerule alone". */
	public static final int NO_TRANSFER_DELAY = -1;

	/**
	 * Slack allowed either side of the expected duration, on top of {@link #TOLERANCE_CYCLES}
	 * delays' worth.
	 * <p>
	 * Absorbs chain latency and the tick the structure is placed on.
	 */
	private static final int TICK_SLACK = 10;

	/**
	 * Cooldowns of leeway allowed either side of the expected duration.
	 * <p>
	 * One covers tests whose assertion is met a transfer early — several are, because they only
	 * wait for an item to reach the last hopper rather than the destination. The second covers
	 * drift between Minecraft versions: the expectations are calibrated on 1.21.1, and 1.20.1 runs
	 * roughly one transfer quicker throughout. Wide enough to survive that, still tight enough to
	 * catch a delay being ignored or doubled.
	 */
	private static final int TOLERANCE_CYCLES = 2;

	/** Hopper cooldowns a test keeps verifying for after its assertions first pass. */
	private static final int SETTLE_CYCLES = 8;

	/** Floor on the settle window, so fast delays still observe a useful stretch of ticks. */
	private static final int MIN_SETTLE_TICKS = 20;

	private static final TestOptions DEFAULT = new TestOptions(
			EMPTY_STRUCTURE, 100, 0L, true, Rotation.NONE, DEFAULT_BATCH, NO_TRANSFER_DELAY, 0, 0
	);

	/**
	 * Default settings: the empty arena, a 100 tick budget, no setup delay, required, unrotated,
	 * in the default batch, leaving the transfer delay gamerule untouched and untimed.
	 *
	 * @return the shared default instance
	 */
	public static TestOptions of() {
		return DEFAULT;
	}

	/**
	 * Declares how long this test should take, so its duration can be asserted rather than merely
	 * capped.
	 * <p>
	 * A hopper moves one item per cooldown, so duration is dominated by the item count times the
	 * delay. Hoppers in a chain tick concurrently rather than in series, so chain length adds only
	 * a small constant — measurements across five delays fit
	 * {@code itemsMoved * delay + hoppers} closely, and notably <em>not</em>
	 * {@code (items + hoppers) * delay}, which overshoots as delay grows.
	 * <p>
	 * {@code itemsMoved} is what the test waits to see transferred, which is the source stack for
	 * most tests but not all: a destination that can only accept part of the source settles once
	 * that part has moved.
	 *
	 * @param itemsMoved items the assertion waits to see moved
	 * @param hoppers    hoppers in the structure's chain
	 * @return a copy carrying that expectation
	 */
	public TestOptions withTiming(int itemsMoved, int hoppers) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}

	/**
	 * @return whether this test's duration should be asserted
	 */
	public boolean isTimed() {
		return itemsMoved > 0;
	}

	/**
	 * @param delay transfer delay in ticks
	 * @return how long this test should take at that delay
	 */
	public int expectedTicks(int delay) {
		return itemsMoved * delay + hoppers;
	}

	/**
	 * @param delay transfer delay in ticks
	 * @return how far either side of {@link #expectedTicks} is still acceptable
	 */
	public int toleranceTicks(int delay) {
		return TOLERANCE_CYCLES * delay + TICK_SLACK;
	}

	/**
	 * How long a test keeps re-checking its assertions after they first pass, before it is allowed
	 * to succeed.
	 * <p>
	 * Succeeding the instant the counts line up would miss an item duplicated or destroyed a moment
	 * later — a hopper mid-transfer holds an item that is briefly in neither container, so the very
	 * first tick the totals agree is not proof they will stay that way. Holding for several more
	 * cooldowns lets every hopper in the chain cycle again under observation.
	 *
	 * @param delay transfer delay in ticks
	 * @return ticks to keep verifying for
	 */
	public int settleTicks(int delay) {
		return Math.max(SETTLE_CYCLES * delay, MIN_SETTLE_TICKS);
	}

	/**
	 * @param structure namespaced id of the structure to run inside
	 * @return a copy using that structure
	 */
	public TestOptions withStructure(String structure) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}

	/**
	 * @param maxTicks tick budget before the runner fails the test
	 * @return a copy with that budget
	 */
	public TestOptions withMaxTicks(int maxTicks) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}

	/**
	 * @param setupTicks delay between structure placement and the body starting
	 * @return a copy with that delay
	 */
	public TestOptions withSetupTicks(long setupTicks) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}

	/**
	 * @param required whether a failure should fail the whole run
	 * @return a copy with that requirement
	 */
	public TestOptions withRequired(boolean required) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}

	/**
	 * @param rotation rotation applied when the structure is placed
	 * @return a copy with that rotation
	 */
	public TestOptions withRotation(Rotation rotation) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}

	/**
	 * @param batch group to run in; tests sharing a batch run concurrently, batches run in turn
	 * @return a copy in that batch
	 */
	public TestOptions withBatch(String batch) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}

	/**
	 * @param transferDelay hopper transfer delay to run under, in ticks
	 * @return a copy running under that delay
	 */
	public TestOptions withTransferDelay(int transferDelay) {
		return new TestOptions(structure, maxTicks, setupTicks, required, rotation, batch, transferDelay, itemsMoved, hoppers);
	}
}
