package gay.bagel.fasthoppers.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Container access for tests, and the only place a version difference leaks into test code.
 * <p>
 * {@code GameTestHelper.getBlockEntity} takes just a position before 1.21.11 and a position plus
 * an expected type from 1.21.11 on. Everything reached through it —
 * {@code BaseContainerBlockEntity}, {@code Container#getContainerSize},
 * {@code Container#getItem} — is identical in both, so confining that one call here keeps every
 * suite version-agnostic.
 */
public final class TestContainers {

	private TestContainers() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Reads the container block entity at a structure-relative position.
	 *
	 * @param helper the arena handle
	 * @param pos    structure-relative position of a chest, hopper, dropper or furnace
	 * @return the container at that position
	 */
	public static BaseContainerBlockEntity container(GameTestHelper helper, BlockPos pos) {
		//? < 1.21.11 {
		// Assigned to BlockEntity first on purpose: 1.20.1 and earlier return BlockEntity outright,
		// while 1.21.1 returns a generic that infers to BlockEntity here. One form covers both.
		BlockEntity blockEntity = helper.getBlockEntity(pos);
		return (BaseContainerBlockEntity) blockEntity;
		//?} >= 1.21.11 {
		/*return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
		*///?}
	}

	/**
	 * Counts one item across the given containers.
	 *
	 * @param helper    the arena handle
	 * @param item      the item to count
	 * @param positions structure-relative positions of the containers to search
	 * @return total number of that item held across all of them
	 */
	public static int count(GameTestHelper helper, Item item, BlockPos... positions) {
		int total = 0;
		for (BlockPos pos : positions) {
			BaseContainerBlockEntity container = container(helper, pos);
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
	 * Fails unless exactly {@code expected} of {@code item} exist across the given containers.
	 * <p>
	 * The core assertion behind this mod's tests: rewriting hopper cooldowns must never create or
	 * destroy items, so every test that moves items checks the total is unchanged rather than only
	 * checking the items arrived.
	 *
	 * @param helper    the arena handle
	 * @param item      the item to account for
	 * @param expected  how many should exist
	 * @param positions every container that could be holding it
	 */
	public static void assertConserved(GameTestHelper helper, Item item, int expected, BlockPos... positions) {
		int actual = count(helper, item, positions);
		if (actual != expected) {
			helper.fail("expected " + expected + " " + item + " across the setup but found " + actual);
		}
	}
}
