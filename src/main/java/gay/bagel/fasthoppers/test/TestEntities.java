package gay.bagel.fasthoppers.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

//? >= 1.21.11 {
/*import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
*///?} < 1.21.11 {
import net.minecraft.world.entity.vehicle.MinecartHopper;
//?}

/**
 * Entity access for tests, and the entity-side counterpart to {@link TestContainers}.
 * <p>
 * Hopper minecarts are entities, so none of the block-entity machinery reaches them, and the
 * obvious routes are not portable: {@code GameTestHelper} renamed {@code getEntities} to
 * {@code findEntities} between 1.19.2 and 1.21.1, and {@code getBounds} is private on the older
 * versions. Going through {@code Level.getEntitiesOfClass} with a box built from
 * {@code absolutePos} avoids both, and is identical on every supported version.
 * <p>
 * The one genuine difference left is where {@code MinecartHopper} lives: it moved into a
 * {@code vehicle.minecart} subpackage in 1.21.11. Confining that to the import here keeps the
 * suites version-agnostic.
 */
public final class TestEntities {

	/**
	 * How far outside the structure a cart is still considered part of the test.
	 * <p>
	 * Minecarts are entities and do not sit perfectly still — carts resting on each other push
	 * apart slightly. A block of slack keeps that from reading as a vanished cart, and is far
	 * short of the spacing the runner leaves between tests in a batch.
	 */
	private static final double DRIFT_ALLOWANCE = 1.0;

	private TestEntities() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Every hopper minecart inside the structure, ordered from lowest to highest.
	 * <p>
	 * Ordering by height is what lets a test talk about "the bottom cart" without hardcoding an
	 * entity order the structure file does not promise.
	 *
	 * @param helper the arena handle
	 * @param sizeX  structure width, as saved
	 * @param sizeY  structure height, as saved
	 * @param sizeZ  structure depth, as saved
	 * @return the carts found, lowest first
	 */
	public static List<MinecartHopper> hopperMinecarts(GameTestHelper helper, int sizeX, int sizeY, int sizeZ) {
		BlockPos one = helper.absolutePos(TestArena.at(0, 0, 0));
		BlockPos two = helper.absolutePos(TestArena.at(sizeX - 1, sizeY - 1, sizeZ - 1));

		// Rotation can put either corner on either side, so the box is built from the extremes
		// rather than assuming one is the minimum.
		AABB box = new AABB(
				Math.min(one.getX(), two.getX()), Math.min(one.getY(), two.getY()), Math.min(one.getZ(), two.getZ()),
				Math.max(one.getX(), two.getX()) + 1, Math.max(one.getY(), two.getY()) + 1, Math.max(one.getZ(), two.getZ()) + 1
		).inflate(DRIFT_ALLOWANCE);

		List<MinecartHopper> carts = helper.getLevel().getEntitiesOfClass(MinecartHopper.class, box);
		carts.sort(Comparator.comparingDouble(cart -> ((MinecartHopper) cart).getY()));
		return carts;
	}

	/**
	 * Counts one item across the given containers.
	 * <p>
	 * Takes {@link Container} rather than a position because a minecart's inventory has no block
	 * to look it up by.
	 *
	 * @param item       the item to count
	 * @param containers the containers to search
	 * @return total number of that item held across all of them
	 */
	public static int count(Item item, List<? extends Container> containers) {
		int total = 0;
		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.is(item)) {
					total += stack.getCount();
				}
			}
		}
		return total;
	}

	/**
	 * Counts one item in a single container.
	 *
	 * @param item      the item to count
	 * @param container the container to search
	 * @return how many of that item it holds
	 */
	public static int count(Item item, Container container) {
		return count(item, List.of(container));
	}

	/**
	 * Fails unless exactly {@code expected} of {@code item} exist across the given containers.
	 * <p>
	 * The same conservation check {@link TestContainers#assertConserved} makes for blocks: moving
	 * items between carts must never create or destroy any.
	 *
	 * @param helper     the arena handle
	 * @param item       the item to account for
	 * @param expected   how many should exist
	 * @param containers every container that could be holding it
	 */
	public static void assertConserved(GameTestHelper helper, Item item, int expected, List<? extends Container> containers) {
		int actual = count(item, containers);
		if (actual != expected) {
			helper.fail("expected " + expected + " " + item + " across the minecarts but found " + actual);
		}
	}

	/**
	 * Describes every cart's state: alive or removed, where it is, and what it holds.
	 *
	 * @param carts the carts to describe
	 * @param item  the item to count in each
	 * @return a one-line summary for a failure message
	 */
	public static String describe(List<MinecartHopper> carts, Item item) {
		StringBuilder summary = new StringBuilder();
		for (int i = 0; i < carts.size(); i++) {
			MinecartHopper cart = carts.get(i);
			summary.append(String.format(
					"cart%d[%s pos=%.2f,%.2f,%.2f items=%d] ",
					i,
					// The removal reason is what distinguishes a cart destroyed in play from one the
					// world simply stopped keeping loaded, which look identical from the outside.
					cart.isRemoved() ? "REMOVED:" + cart.getRemovalReason() : "alive",
					cart.getX(), cart.getY(), cart.getZ(),
					count(item, cart)
			));
		}
		return summary.toString().trim();
	}

	/**
	 * Fails if any of the captured carts has been removed from the world.
	 * <p>
	 * Checked separately from the item counts because a cart leaving the world takes its contents
	 * with it, which would otherwise surface as items mysteriously failing to be conserved. The
	 * message carries the tick it happened on and every cart's position and contents, so the failure
	 * says what state the arena reached rather than leaving it to be reconstructed.
	 *
	 * @param helper  the arena handle
	 * @param carts   the carts captured at the start of the test
	 * @param item    the item being tracked, for the contents report
	 * @param elapsed ticks since the test body started
	 */
	public static void assertCartsIntact(GameTestHelper helper, List<MinecartHopper> carts, Item item, long elapsed) {
		for (int i = 0; i < carts.size(); i++) {
			if (carts.get(i).isRemoved()) {
				helper.fail("minecart " + i + " was removed from the world on tick " + elapsed
						+ " of this test; " + describe(carts, item));
			}
		}
	}

	/**
	 * Fails unless the structure holds exactly the number of carts the test was written against.
	 * <p>
	 * Checked explicitly because every later assertion indexes into the list: a cart that fell out
	 * of the arena would otherwise surface as a confusing count mismatch, or an index error.
	 *
	 * @param helper   the arena handle
	 * @param carts    the carts found
	 * @param expected how many the structure should contain
	 */
	public static void assertCartCount(GameTestHelper helper, List<MinecartHopper> carts, int expected) {
		if (carts.size() != expected) {
			helper.fail("expected " + expected + " hopper minecarts in the arena but found " + carts.size());
		}
	}
}
