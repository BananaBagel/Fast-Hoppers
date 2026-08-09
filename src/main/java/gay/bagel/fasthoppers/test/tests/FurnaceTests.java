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
 * Hoppers feeding a furnace, which is the interesting case because the slot an item lands in
 * depends on the face it arrives through: from above it goes to the input slot, from the side to
 * the fuel slot.
 * <p>
 * Wool is used throughout because it is a valid fuel (it burns for 100 ticks), so it is accepted
 * through either face. Nothing is ever consumed, because a furnace only burns when it has both
 * fuel and something to smelt, and wool has no smelting recipe — which keeps the counts stable
 * enough to assert on.
 */
public final class FurnaceTests {

	/** Source chest above the vertical chain. */
	private static final BlockPos TOP_SOURCE = at(0, 3, 0);
	/** Hopper sitting directly on the furnace, feeding its input slot. */
	private static final BlockPos TOP_HOPPER = at(0, 2, 0);
	/** The furnace, in the top-fed layout. */
	private static final BlockPos TOP_FURNACE = at(0, 1, 0);

	/** Source chest in the side-fed layout. */
	private static final BlockPos FUEL_SOURCE = at(0, 3, 1);
	/** Upper hopper in the side-fed layout. */
	private static final BlockPos FUEL_UPPER = at(0, 2, 1);
	/** Hopper that pushes north into the furnace's side, targeting the fuel slot. */
	private static final BlockPos FUEL_PUSHER = at(0, 1, 1);
	/** The furnace, in the side-fed layout. */
	private static final BlockPos FUEL_FURNACE = at(0, 1, 0);

	private FurnaceTests() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Registers this suite's tests.
	 *
	 * @param tests sink supplied by the caller; see {@link TestRegistrar}
	 */
	public static void contribute(TestRegistrar tests) {
		tests.add("regular_furnace_top", arena("regular_furnace_top").withTiming(64, 1), FurnaceTests::regularFurnaceTop);
		tests.add("full_furnace_top", arena("full_furnace_top").withTiming(64, 1), FurnaceTests::fullFurnaceTop);
		tests.add("regular_furnace_fuel", arena("regular_furnace_fuel").withTiming(64, 2), FurnaceTests::regularFurnaceFuel);
		tests.add("full_furnace_fuel", arena("full_furnace_fuel").withTiming(64, 2), FurnaceTests::fullFurnaceFuel);
	}

	/**
	 * Empty furnace fed from directly above, which targets its input slot.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void regularFurnaceTop(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, TOP_SOURCE), () -> {
			TestContainers.assertConserved(helper, wool, 64, TOP_SOURCE, TOP_HOPPER, TOP_FURNACE);
			assertHolds(helper, wool, 64, TOP_FURNACE, "the furnace input slot");
		});
	}

	/**
	 * Furnace whose input slot already holds a full stack, so nothing more fits. The blocked items
	 * should collect in the hopper above rather than disappear.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void fullFurnaceTop(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, TOP_SOURCE), () -> {
			TestContainers.assertConserved(helper, wool, 128, TOP_SOURCE, TOP_HOPPER, TOP_FURNACE);
			assertHolds(helper, wool, 64, TOP_FURNACE, "the full furnace");
			assertHolds(helper, wool, 64, TOP_HOPPER, "the hopper above the furnace");
		});
	}

	/**
	 * Empty furnace fed from the side, which targets the fuel slot. Wool is valid fuel, so the
	 * stack is accepted.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void regularFurnaceFuel(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, FUEL_SOURCE), () -> {
			TestContainers.assertConserved(helper, wool, 64, FUEL_SOURCE, FUEL_UPPER, FUEL_PUSHER, FUEL_FURNACE);
			assertHolds(helper, wool, 64, FUEL_FURNACE, "the furnace fuel slot");
		});
	}

	/**
	 * Furnace whose fuel slot already holds a full stack, so no more will fit. The rejected items
	 * must back up into the hopper rather than be destroyed.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void fullFurnaceFuel(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, FUEL_SOURCE), () -> {
			TestContainers.assertConserved(helper, wool, 128, FUEL_SOURCE, FUEL_UPPER, FUEL_PUSHER, FUEL_FURNACE);
			assertHolds(helper, wool, 64, FUEL_FURNACE, "the full fuel slot");
			assertHolds(helper, wool, 64, FUEL_PUSHER, "the hopper feeding the furnace");
		});
	}
}
