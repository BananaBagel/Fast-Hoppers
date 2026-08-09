package gay.bagel.fasthoppers.test.tests;

import gay.bagel.fasthoppers.test.TestArena;
import gay.bagel.fasthoppers.test.TestContainers;
import gay.bagel.fasthoppers.test.TestItems;
import gay.bagel.fasthoppers.test.TestRegistrar;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import static gay.bagel.fasthoppers.test.TestArena.arena;
import static gay.bagel.fasthoppers.test.TestArena.assertHolds;
import static gay.bagel.fasthoppers.test.TestArena.at;
import static gay.bagel.fasthoppers.test.TestArena.succeedWhen;

/**
 * Hoppers pushing sideways into a dropper.
 * <p>
 * All three share a layout — source chest, two hoppers, dropper to the west — and differ only in
 * what the dropper already holds, which is what decides whether insertion is possible.
 */
public final class DropperTests {

	/** Source chest at the top of the chain. */
	private static final BlockPos SOURCE = at(1, 3, 0);
	/** Hopper directly below the source chest. */
	private static final BlockPos UPPER = at(1, 2, 0);
	/** Hopper that pushes west into the dropper. */
	private static final BlockPos PUSHER = at(1, 1, 0);
	/** The dropper under test. */
	private static final BlockPos DROPPER = at(0, 1, 0);

	private DropperTests() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Registers this suite's tests.
	 *
	 * @param tests sink supplied by the caller; see {@link TestRegistrar}
	 */
	public static void contribute(TestRegistrar tests) {
		tests.add("regular_dropper", arena("regular_dropper").withTiming(64, 2), DropperTests::regularDropper);
		tests.add("full_dropper", arena("full_dropper").withTiming(64, 2), DropperTests::fullDropper);
		tests.add("random_full_dropper", arena("random_full_dropper").withTiming(64, 2), DropperTests::randomFullDropper);
	}

	/**
	 * Empty dropper accepting a stack pushed sideways out of a hopper.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void regularDropper(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, SOURCE), () -> {
			TestContainers.assertConserved(helper, wool, 64, SOURCE, UPPER, PUSHER, DROPPER);
			assertHolds(helper, wool, 64, DROPPER, "the dropper");
		});
	}

	/**
	 * Dropper already holding nine full stacks, so nothing can be inserted.
	 * <p>
	 * The point is that the blocked items are not destroyed: they should back up into the hopper
	 * feeding the dropper, leaving the grand total untouched.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void fullDropper(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, SOURCE), () -> {
			// Nine full stacks already in the dropper, plus the source stack.
			TestContainers.assertConserved(helper, wool, 640, SOURCE, UPPER, PUSHER, DROPPER);
			assertHolds(helper, wool, 576, DROPPER, "the full dropper");
			assertHolds(helper, wool, 64, PUSHER, "the hopper feeding the dropper");
		});
	}

	/**
	 * Dropper full of assorted wool colours, so every slot is occupied but none can stack with the
	 * incoming white wool. Catches a hopper voiding an item when no slot will accept it.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void randomFullDropper(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);
		BlockPos[] all = {SOURCE, UPPER, PUSHER, DROPPER};

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, SOURCE), () -> {
			TestContainers.assertConserved(helper, wool, 64, all);
			assertHolds(helper, wool, 0, DROPPER, "the colour-filled dropper");
			assertHolds(helper, wool, 64, PUSHER, "the hopper feeding the dropper");

			// The dropper's existing contents must be untouched too.
			TestContainers.assertConserved(helper, TestItems.wool(DyeColor.PINK), 91, all);
			TestContainers.assertConserved(helper, TestItems.wool(DyeColor.RED), 55, all);
			TestContainers.assertConserved(helper, TestItems.wool(DyeColor.ORANGE), 90, all);
			TestContainers.assertConserved(helper, TestItems.wool(DyeColor.LIME), 64, all);
		});
	}
}
