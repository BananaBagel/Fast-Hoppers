package gay.bagel.fasthoppers.platform.fabric.datagen;

//? fabric {

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Datagen entry point, referenced by the generated {@code fabric.mod.json}.
 * <p>
 * Datagen runs on Fabric only, but its output is shared: the Forge and NeoForge buildscripts add
 * the same generated directory to their resources, so one run covers every loader for a given
 * Minecraft version.
 */
public class FabricDataGeneratorEntrypoint implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		//? >= 1.21.11 {
		/*// Before 1.21.11 tests are declared entirely in code, so there is nothing to generate.
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(GameTestDataProvider::new);
		*///?}
	}
}
//?}
