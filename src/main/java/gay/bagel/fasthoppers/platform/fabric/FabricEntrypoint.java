package gay.bagel.fasthoppers.platform.fabric;

//? fabric {

import gay.bagel.fasthoppers.FastHoppers;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;

@Entrypoint("main")
public class FabricEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		FastHoppers.onInitialize();
	}
}
//?}
