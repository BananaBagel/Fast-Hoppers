package gay.bagel.fasthoppers;

//? fabric {
//? < 1.21.11 {
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
//?} >= 1.21.11 {

/*import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
		*///?}
//?}


import net.minecraft.server.MinecraftServer;
//? < 1.21.11 {
import net.minecraft.world.level.GameRules;
 //?} >= 1.21.11 {
/*import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.resources.ResourceLocation;
import gay.bagel.fasthoppers.FastHoppers;
*///?}
//? >= 1.21.11
//import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class FastHoppersGameRules {

	private static final /*? >= 1.21.11 {*/ /*ResourceLocation *//*?} < 1.21.11 {*/ String /*?}*/ HOPPER_TRANSFER_DELAY_ID = /*? >= 1.21.11 {*/ /*FastHoppers.id("hopper_transfer_delay"); *//*?} < 1.21.11 {*/ "hopperTransferDelay";/*?}*/
	private static final /*? >= 1.21.11 {*/ /*ResourceLocation *//*?} < 1.21.11 {*/ String /*?}*/ HOPPER_MINECART_TRANSFER_DELAY_ID = /*? >= 1.21.11 {*/ /*FastHoppers.id("hopper_minecart_transfer_delay"); *//*?} < 1.21.11 {*/ "hopperMinecartTransferDelay"; /*?}*/

	/**
	 * Ticks between transfer actions for hoppers. Vanilla behavior is 8.
	 */
	public static /*? >= 1.21.11 {*//*GameRule<Integer>*//*?} < 1.21.11 {*/ GameRules.Key<GameRules.IntegerValue> /*?}*/ HOPPER_TRANSFER_DELAY;
	/**
	 * Ticks between transfer actions for hopper minecarts. Vanilla behavior is 1, or it's weird depending on the version.
	 */
	public static /*? >= 1.21.11 {*//*GameRule<Integer>*//*?} < 1.21.11 {*/ GameRules.Key<GameRules.IntegerValue> /*?}*/ HOPPER_MINECART_TRANSFER_DELAY;

	/**
	 * Register the gamerules into the gamerule registry.<br>
	 * Called from entrypoint.
	 *
	 */
	public static void RegisterGameRules() {
		//? < 1.21.11 {
		HOPPER_TRANSFER_DELAY = GameRules.register(
				HOPPER_TRANSFER_DELAY_ID,
				GameRules.Category.UPDATES,
				GameRules.IntegerValue.create(8)
		);
		HOPPER_MINECART_TRANSFER_DELAY = GameRules.register(
				HOPPER_MINECART_TRANSFER_DELAY_ID,
				GameRules.Category.UPDATES,
				GameRules.IntegerValue.create(1)
		);
		//?} >= 1.21.11 {
		/*//? fabric {
		HOPPER_TRANSFER_DELAY = GameRuleBuilder
				.forInteger(8)
				.category(GameRuleCategory.UPDATES)
				.buildAndRegister(HOPPER_TRANSFER_DELAY_ID);
		HOPPER_MINECART_TRANSFER_DELAY = GameRuleBuilder
				.forInteger(1)
				.category(GameRuleCategory.UPDATES)
				.buildAndRegister(HOPPER_MINECART_TRANSFER_DELAY_ID);
		//?} neoforge {
		/^HOPPER_MINECART_TRANSFER_DELAY = GameRules.registerInteger(HOPPER_MINECART_TRANSFER_DELAY_ID.toShortString(), GameRuleCategory.UPDATES, 1, 1);
		HOPPER_TRANSFER_DELAY = GameRules.registerInteger(HOPPER_TRANSFER_DELAY_ID.toShortString(), GameRuleCategory.UPDATES, 8, 1);


		^///?}
		*///?}
	}

	private FastHoppersGameRules() {
		throw new AssertionError("No instances.");
	}

	/**
	 * The smallest delay either rule will act on.
	 * <p>
	 * One tick is already the fastest a hopper can move anything, so nothing below it means
	 * anything. It is enforced when the value is read rather than where the rules are registered
	 * because only one of the three registration paths can express a minimum: NeoForge takes one
	 * as an argument, Fabric's builder does not, and vanilla's legacy {@code IntegerValue} has no
	 * concept of one at all. Clamping here is the only place that covers every loader and version,
	 * so the mod cannot behave differently depending on where it is installed.
	 */
	public static final int MINIMUM_DELAY = 1;

	/**
	 * Reads {@code hopperTransferDelay} for a level, never below {@link #MINIMUM_DELAY}.
	 *
	 * @param level the level whose rules to read
	 * @return the configured delay in ticks
	 */
	public static int transferDelay(Level level) {
		//? < 1.21.11
		return Math.max(MINIMUM_DELAY, level.getGameRules().getInt(HOPPER_TRANSFER_DELAY));
		//? >= 1.21.11
		//return Math.max(MINIMUM_DELAY, ((ServerLevel) level).getGameRules().get(HOPPER_TRANSFER_DELAY));
	}

	/**
	 * Reads {@code hopperMinecartTransferDelay} for a level, never below {@link #MINIMUM_DELAY}.
	 *
	 * @param level the level whose rules to read
	 * @return the configured delay in ticks
	 */
	public static int minecartTransferDelay(Level level) {
		//? < 1.21.11
		return Math.max(MINIMUM_DELAY, level.getGameRules().getInt(HOPPER_MINECART_TRANSFER_DELAY));
		//? >= 1.21.11
		//return Math.max(MINIMUM_DELAY, ((ServerLevel) level).getGameRules().get(HOPPER_MINECART_TRANSFER_DELAY));
	}

	/**
	 * Sets {@code hopperTransferDelay} on a level. Used by the GameTests.
	 *
	 * @param level  the level whose rule to change
	 * @param ticks  the delay to apply
	 * @param server the server that owns the level, for rule-change callbacks
	 */
	public static void setTransferDelay(Level level, int ticks, MinecraftServer server) {
		//? < 1.21.11
		level.getGameRules().getRule(HOPPER_TRANSFER_DELAY).set(ticks, server);
		//? >= 1.21.11
		//server.getLevel(level.dimension()).getGameRules().set(HOPPER_TRANSFER_DELAY, ticks, server);
	}

	/**
	 * Sets {@code hopperMinecartTransferDelay} on a level. Used by the GameTests.
	 *
	 * @param level  the level whose rule to change
	 * @param ticks  the delay to apply
	 * @param server the server that owns the level, for rule-change callbacks
	 */
	public static void setMinecartTransferDelay(Level level, int ticks, MinecraftServer server) {
		//? < 1.21.11
		level.getGameRules().getRule(HOPPER_MINECART_TRANSFER_DELAY).set(ticks, server);
		//? >= 1.21.11
		//server.getLevel(level.dimension()).getGameRules().set(HOPPER_MINECART_TRANSFER_DELAY, ticks, server);
	}
}
