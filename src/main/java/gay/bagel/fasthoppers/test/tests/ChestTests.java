package gay.bagel.fasthoppers.test.tests;

import gay.bagel.fasthoppers.test.TestArena;
import gay.bagel.fasthoppers.test.TestContainers;
import gay.bagel.fasthoppers.test.TestItems;
import gay.bagel.fasthoppers.test.TestRegistrar;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import static gay.bagel.fasthoppers.test.TestArena.arena;
import static gay.bagel.fasthoppers.test.TestArena.assertHolds;
import static gay.bagel.fasthoppers.test.TestArena.at;
import static gay.bagel.fasthoppers.test.TestArena.succeedWhen;

/**
 * Hoppers moving items into and between chests.
 * <p>
 * Each test asserts conservation as well as arrival: this mod rewrites hopper transfer cooldowns,
 * and the failure that matters is an item being duplicated or destroyed in transit.
 */
public final class ChestTests {

	private ChestTests() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Registers this suite's tests.
	 *
	 * @param tests sink supplied by the caller; see {@link TestRegistrar}
	 */
	public static void contribute(TestRegistrar tests) {
		tests.add("basic_chest", arena("basic_chest").withTiming(64, 1), ChestTests::basicChest);
		tests.add("chest_storage", arena("chest_storage").withTiming(64, 3), ChestTests::chestStorage);
		tests.add("semifull_chest", arena("semifull_chest").withTiming(128, 2), ChestTests::semifullChest);
	}

	/**
	 * Source chest, one downward hopper, empty destination chest: the whole stack should arrive.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void basicChest(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);
		BlockPos source = at(0, 3, 0);
		BlockPos hopper = at(0, 2, 0);
		BlockPos destination = at(0, 1, 0);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, source), () -> {
			TestContainers.assertConserved(helper, wool, 64, source, hopper, destination);
			assertHolds(helper, wool, 64, destination, "the destination chest");
		});
	}

	/**
	 * Longer branching chain into two chests. Only conservation is asserted, because the split
	 * between the two destinations depends on hopper cooldown ordering and is not fixed.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void chestStorage(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);
		BlockPos source = at(0, 4, 1);
		BlockPos[] all = {
				source,
				at(0, 3, 1), at(0, 2, 1), at(0, 1, 1),
				at(0, 2, 0), at(0, 1, 0)
		};

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, source), () -> {
			TestContainers.assertConserved(helper, wool, 64, all);
			assertHolds(helper, wool, 0, source, "the source chest");
		});
	}

	/**
	 * Destination chest with a single free slot: one stack fits, the second must back up rather
	 * than vanish. The shovels occupying the other slots must not be disturbed.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void semifullChest(GameTestHelper helper) {
		Item wool = TestItems.wool(DyeColor.WHITE);
		BlockPos source = at(1, 3, 0);
		BlockPos upper = at(1, 2, 0);
		BlockPos pusher = at(1, 1, 0);
		BlockPos destination = at(0, 1, 0);
		BlockPos[] all = {source, upper, pusher, destination};

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, source), () -> {
			TestContainers.assertConserved(helper, wool, 128, all);
			assertHolds(helper, wool, 64, destination, "the nearly full chest");
			assertHolds(helper, wool, 64, pusher, "the hopper behind it");
			TestContainers.assertConserved(helper, Items.DIAMOND_SHOVEL, 26, all);
		});
	}
}
