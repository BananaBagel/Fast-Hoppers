package gay.bagel.fasthoppers.test;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Item lookups that differ between versions, kept out of the test bodies.
 * <p>
 * Minecraft 26.2 replaced the sixteen per-colour constants such as {@code Items.WHITE_WOOL} with a
 * single {@code ColorCollection} indexed by {@link DyeColor}. Routing colour lookups through here
 * means a suite can name a colour once and compile on either side of that change. 26.1.2 still has
 * the old constants, so the split is at 26.2 rather than at the 26 line as a whole.
 */
public final class TestItems {

	private TestItems() {
		throw new AssertionError("No instances.");
	}

	/**
	 * The wool item of a given colour.
	 *
	 * @param colour the dye colour
	 * @return that colour's wool
	 */
	public static Item wool(DyeColor colour) {
		//? < 26.2 {
		return switch (colour) {
			case WHITE -> Items.WHITE_WOOL;
			case ORANGE -> Items.ORANGE_WOOL;
			case MAGENTA -> Items.MAGENTA_WOOL;
			case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
			case YELLOW -> Items.YELLOW_WOOL;
			case LIME -> Items.LIME_WOOL;
			case PINK -> Items.PINK_WOOL;
			case GRAY -> Items.GRAY_WOOL;
			case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
			case CYAN -> Items.CYAN_WOOL;
			case PURPLE -> Items.PURPLE_WOOL;
			case BLUE -> Items.BLUE_WOOL;
			case BROWN -> Items.BROWN_WOOL;
			case GREEN -> Items.GREEN_WOOL;
			case RED -> Items.RED_WOOL;
			case BLACK -> Items.BLACK_WOOL;
		};
		//?} >= 26.2 {
		/*return Items.WOOL.pick(colour);
		*///?}
	}
}
