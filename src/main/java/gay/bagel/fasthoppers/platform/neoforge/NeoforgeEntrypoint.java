package gay.bagel.fasthoppers.platform.neoforge;

//? neoforge {

/*import gay.bagel.fasthoppers.FastHoppers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//? >= 1.21.11 {
import gay.bagel.fasthoppers.FastHoppersGameRules;
import gay.bagel.fasthoppers.test.FastHoppersGameTests;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;
//?}

@Mod(FastHoppers.MOD_ID)
public class NeoforgeEntrypoint {

	/^*
	 * @param modBus this mod's event bus, injected by NeoForge
	 ^/
	public NeoforgeEntrypoint(IEventBus modBus) {
		//? >= 1.21.11 {
		// From 1.21.11 both gamerules and gametest functions live in registries that are frozen
		// everywhere outside RegisterEvent — including in here — so registration waits for the event.
		modBus.addListener(RegisterEvent.class, event -> {
			event.register(Registries.GAME_RULE, helper -> FastHoppersGameRules.RegisterGameRules());
			event.register(Registries.TEST_FUNCTION, helper ->
					FastHoppersGameTests.registerFunctions(helper::register));
		});
		//?}
		FastHoppers.onInitialize();
	}
}
*///?}
