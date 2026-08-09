package gay.bagel.fasthoppers.test.tests;

import gay.bagel.fasthoppers.test.TestArena;
import gay.bagel.fasthoppers.test.TestContainers;
import gay.bagel.fasthoppers.test.TestItems;
import gay.bagel.fasthoppers.test.TestOptions;
import gay.bagel.fasthoppers.test.TestRegistrar;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static gay.bagel.fasthoppers.test.TestArena.arena;
import static gay.bagel.fasthoppers.test.TestArena.assertHolds;
import static gay.bagel.fasthoppers.test.TestArena.at;
import static gay.bagel.fasthoppers.test.TestArena.succeedWhen;

/**
 * Hoppers in isolation, and hoppers feeding other hoppers.
 * <p>
 * Tests that target a specific destination block type live in their own suite —
 * {@link ChestTests}, {@link DropperTests}, {@link FurnaceTests}.
 */
public final class HopperTests {

	/** Middle of the empty arena's bottom layer — clear of every wall. */
	private static final BlockPos CENTER = new BlockPos(1, 1, 1);

	/**
	 * How many loose items the pickup test drops.
	 * <p>
	 * More than one on purpose: counting them on the way in is what catches an item being voided
	 * or duplicated during pickup, which a presence check cannot see. Kept under a stack so the
	 * whole lot fits in a single hopper slot.
	 */
	private static final int DROPPED_ITEMS = 24;

	/** Source chest at the top of the hopper-to-hopper chain. */
	private static final BlockPos SOURCE = at(1, 3, 0);
	/** Hopper directly below the source chest. */
	private static final BlockPos UPPER = at(1, 2, 0);
	/** Hopper that pushes west into the destination hopper. */
	private static final BlockPos PUSHER = at(1, 1, 0);
	/** The destination hopper, which cannot drain because it faces bedrock. */
	private static final BlockPos DESTINATION = at(0, 1, 0);

	private HopperTests() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Registers this suite's tests.
	 *
	 * @param tests sink supplied by the caller; see {@link TestRegistrar}
	 */
	public static void contribute(TestRegistrar tests) {
		tests.add("lone_hopper", HopperTests::loneHopper);
		tests.add("collects_dropped_item", TestOptions.of().withMaxTicks(400), HopperTests::collectsDroppedItem);
		tests.add("full_hopper", arena("full_hopper").withTiming(64, 2), HopperTests::fullHopper);
	}

	/**
	 * Places a single hopper in an otherwise empty arena and passes if it is still there.
	 * <p>
	 * Deliberately trivial: this exercises registration, structure lookup and result reporting end
	 * to end, so a failure points at the harness rather than at hopper behaviour.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void loneHopper(GameTestHelper helper) {
		helper.setBlock(CENTER, Blocks.HOPPER);
		helper.assertBlockPresent(Blocks.HOPPER, CENTER);
		helper.succeed();
	}

	/**
	 * Drops a known number of loose items above a hopper and passes once every one of them is
	 * inside it.
	 * <p>
	 * This is the only test covering entity pickup — the structures all exercise container to
	 * container transfer — and pickup runs on the same cooldown this mod rewrites. Counting the
	 * items rather than checking they arrived is the point: a voided or duplicated item is exactly
	 * the failure worth catching, and a presence check would miss it.
	 * <p>
	 * Items spawn at the center of the space above the hopper rather than on a block corner, so
	 * they reliably fall within its pickup area.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void collectsDroppedItem(GameTestHelper helper) {
		helper.setBlock(CENTER, Blocks.HOPPER);
		for (int i = 0; i < DROPPED_ITEMS; i++) {
			helper.spawnItem(Items.STONE, CENTER.getX() + 0.5f, CENTER.getY() + 1.5f, CENTER.getZ() + 0.5f);
		}

		succeedWhen(helper, () -> TestContainers.count(helper, Items.STONE, CENTER), () -> {
			assertHolds(helper, Items.STONE, DROPPED_ITEMS, CENTER, "the hopper");
			// Nothing may be left lying above it either, which is what would show if a pickup were
			// counted but not consumed.
			helper.assertItemEntityNotPresent(Items.STONE, CENTER.above(), 2.0);
		});
	}

	/**
	 * Hopper pushing sideways into another hopper that has no free slot, so the items must back up.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void fullHopper(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, SOURCE), () -> {
			// Five full slots in the destination hopper, plus the source stack.
			TestContainers.assertConserved(helper, wool, 384, SOURCE, UPPER, PUSHER, DESTINATION);
			assertHolds(helper, wool, 64, PUSHER, "the hopper feeding the full hopper");
		});
	}
}
