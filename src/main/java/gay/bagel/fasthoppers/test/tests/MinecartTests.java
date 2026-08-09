package gay.bagel.fasthoppers.test.tests;

import gay.bagel.fasthoppers.FastHoppersGameRules;
import gay.bagel.fasthoppers.test.TestArena;
import gay.bagel.fasthoppers.test.TestContainers;
import gay.bagel.fasthoppers.test.TestEntities;
import gay.bagel.fasthoppers.test.TestItems;
import gay.bagel.fasthoppers.test.TestOptions;
import gay.bagel.fasthoppers.test.TestRegistrar;
import gay.bagel.fasthoppers.test.TestTiming;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.util.List;

//? >= 1.21.11 {
/*import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
*///?} < 1.21.11 {
import net.minecraft.world.entity.vehicle.MinecartHopper;
//?}

import static gay.bagel.fasthoppers.test.TestArena.arena;
import static gay.bagel.fasthoppers.test.TestArena.assertHolds;
import static gay.bagel.fasthoppers.test.TestArena.at;
import static gay.bagel.fasthoppers.test.TestArena.succeedWhen;

/**
 * Hopper minecarts picking items up, under {@code hopperMinecartTransferDelay}.
 * <p>
 * These are the only tests that exercise the entity side of the mod, and the only ones whose
 * structures contain entities at all. As everywhere else, arrival is asserted alongside
 * conservation: the cooldown this mod adds must not lose or duplicate anything in transit.
 * <p>
 * Neither structure contains a hopper block, which is deliberate — it means a failure here can
 * only come from the minecart path, and it doubles as a check that the two gamerules are
 * genuinely independent.
 */
public final class MinecartTests {

	private MinecartTests() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Registers this suite's tests.
	 * <p>
	 * Left untimed for now: the duration model in {@code TestOptions} is derived from hoppers in a
	 * chain, and a stack of carts is not that shape. The measured durations are still logged on
	 * every run, which is what calibrating a model for them will need.
	 *
	 * @param tests sink supplied by the caller; see {@link TestRegistrar}
	 */
	public static void contribute(TestRegistrar tests) {
		// Kept out of the shared batch. These are the only arenas holding entities, and they are
		// narrow enough that a neighbouring test's cleanup can reach in and kill the carts while
		// this test is still running. Running the minecart tests only alongside each other removes
		// that entirely; see the batching note in FastHoppersGameTests.
		// Timed now that the chain is genuinely serial. It was not before: with the carts resting on
		// each other the bottom one sat inside the pickup range of both carts above it, so items
		// arrived at roughly two thirds of a delay each and no plain "items x delay" figure fitted.
		// Separating them onto slabs put each cart in only its neighbour's range, and the measured
		// rate now matches the configured delay on all six versions.
		tests.add("minecart_chain", arena("hopper_minecart_chain").withBatch("minecart").withTiming(64, 3), MinecartTests::minecartChain);
		tests.add("minecart_input", arena("hopper_minecart_input").withBatch("minecart").withTiming(64, 1), MinecartTests::minecartInput);
	}

	/**
	 * Points the delay this run is fanned out over at the minecart gamerule.
	 * <p>
	 * The fan-out in {@code FastHoppersGameTests} drives {@code hopperTransferDelay}, which no
	 * structure here reacts to. Re-applying the same value to the minecart rule is what puts these
	 * tests through the full spread of delays rather than running five copies of the default.
	 *
	 * @param helper the arena handle
	 */
	private static void applyMinecartDelay(GameTestHelper helper) {
		int delay = TestTiming.currentOptions().transferDelay();
		if (delay == TestOptions.NO_TRANSFER_DELAY) {
			return;
		}
		FastHoppersGameRules.setMinecartTransferDelay(helper.getLevel(), delay, helper.getLevel().getServer());
	}

	/**
	 * Three hopper minecarts stacked on each other, a full stack of wool in the top one.
	 * <p>
	 * Each cart pulls from the one above it, so the wool should walk down the stack and end up
	 * entirely in the bottom cart, with nothing lost on either hop.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void minecartChain(GameTestHelper helper) {
		applyMinecartDelay(helper);
		Item wool = TestItems.wool(DyeColor.WHITE);

		// Captured once, while the carts are still where the structure put them. Nothing holds this
		// stack together, so over a long run they shove each other apart and wander; re-finding them
		// by position every tick would report a drifting cart as a missing one.
		List<MinecartHopper> carts = TestEntities.hopperMinecarts(helper, 1, 4, 1);
		TestEntities.assertCartCount(helper, carts, 3);
		long start = helper.getTick();

		succeedWhen(helper, () -> 64 - TestEntities.count(wool, carts.get(2)), () -> {
			TestEntities.assertCartsIntact(helper, carts, wool, helper.getTick() - start);
			TestEntities.assertConserved(helper, wool, 64, carts);

			int bottom = TestEntities.count(wool, carts.get(0));
			if (bottom != 64) {
				helper.fail("the bottom minecart should hold 64 " + wool + " but holds " + bottom);
			}
		});
	}

	/**
	 * A hopper minecart parked on a rail underneath a full chest: it should empty the chest into
	 * itself.
	 *
	 * @param helper the arena handle supplied by the test runner
	 */
	private static void minecartInput(GameTestHelper helper) {
		applyMinecartDelay(helper);
		Item wool = TestItems.wool(DyeColor.WHITE);
		BlockPos chest = at(0, 2, 0);

		// Captured once for the same reason as the chain test; this cart sits on a rail and should
		// stay put, but there is no reason to depend on that.
		List<MinecartHopper> carts = TestEntities.hopperMinecarts(helper, 1, 3, 1);
		TestEntities.assertCartCount(helper, carts, 1);

		succeedWhen(helper, TestArena.movedFrom(helper, wool, 64, chest), () -> {
			int inChest = TestContainers.count(helper, wool, chest);
			int inCart = TestEntities.count(wool, carts.get(0));
			if (inChest + inCart != 64) {
				helper.fail("expected 64 " + wool + " between the chest and the cart but found "
						+ (inChest + inCart));
			}

			assertHolds(helper, wool, 0, chest, "the source chest");
			if (inCart != 64) {
				helper.fail("the minecart should hold 64 " + wool + " but holds " + inCart);
			}
		});
	}
}
