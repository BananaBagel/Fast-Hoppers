package gay.bagel.fasthoppers.platform.fabric;

//? fabric {

import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import gay.bagel.fasthoppers.FastHoppers;
import net.fabricmc.api.ClientModInitializer;

@Entrypoint("main")
public class FabricClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		FastHoppers.onInitializeClient();
	}
}
//?}
