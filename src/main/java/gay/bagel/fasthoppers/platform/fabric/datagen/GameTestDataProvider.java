package gay.bagel.fasthoppers.platform.fabric.datagen;

//? fabric && >= 1.21.11 {

/*import com.google.gson.JsonObject;
import gay.bagel.fasthoppers.FastHoppers;
import gay.bagel.fasthoppers.test.FastHoppersGameTests;
import gay.bagel.fasthoppers.test.TestOptions;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/^*
 * Writes the datapack entries the 1.21.11+ gametest system needs.
 *
 * <p>
 * From 1.21.11 a test is only half code: the runtime registers a function, and a
 * {@code test_instance} entry decides which structure it runs in and for how long. Writing those
 * by hand would mean restating every test in JSON and keeping the two in step by memory — and with
 * each test fanned out across five transfer delays there are far too many to maintain.
 *
 * <p>
 * So this walks {@link FastHoppersGameTests#collect} exactly as the two runtimes do, and turns the
 * same declarations into files. Because all three consumers read one list, a test cannot exist for
 * one of them and be missing from another.
 *
 * <p>
 * It also emits one {@code test_environment} per delay. Beyond applying the gamerule, those give
 * the isolation the tests depend on: gamerules are per level and tests sharing an environment run
 * at the same time, so each delay needs its own. This is the 1.21.11+ counterpart to the batch
 * names the older runtime uses.
 ^/
public class GameTestDataProvider implements DataProvider {

	/^* Gamerule set by the generated environments; matches FastHoppersGameRules. ^/
	private static final String TRANSFER_DELAY_RULE = FastHoppers.MOD_ID + ":hopper_transfer_delay";

	private final PackOutput.PathProvider instances;
	private final PackOutput.PathProvider environments;

	/^*
	 * @param output where the generated pack is written
	 ^/
	public GameTestDataProvider(FabricDataOutput output) {
		this.instances = output.createRegistryElementsPathProvider(Registries.TEST_INSTANCE);
		this.environments = output.createRegistryElementsPathProvider(Registries.TEST_ENVIRONMENT);
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		List<CompletableFuture<?>> writes = new ArrayList<>();
		Set<Integer> delays = new LinkedHashSet<>();

		FastHoppersGameTests.collect((name, options, body) -> {
			delays.add(options.transferDelay());
			writes.add(DataProvider.saveStable(cache, instance(name, options), instances.json(FastHoppers.id(name))));
		});

		for (int delay : delays) {
			ResourceLocation id = FastHoppers.id(environmentName(delay));
			writes.add(DataProvider.saveStable(cache, environment(delay), environments.json(id)));
		}

		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	/^*
	 * Builds one {@code test_instance} entry.
	 *
	 * @param name    the test's id path
	 * @param options settings the runtime does not carry on this version
	 * @return the entry as JSON
	 ^/
	private static JsonObject instance(String name, TestOptions options) {
		JsonObject json = new JsonObject();
		json.addProperty("type", "minecraft:function");
		json.addProperty("function", FastHoppers.MOD_ID + ":" + name);
		json.addProperty("structure", options.structure());
		json.addProperty("environment", FastHoppers.MOD_ID + ":" + environmentName(options.transferDelay()));
		json.addProperty("max_ticks", options.maxTicks());
		if (options.setupTicks() > 0L) {
			json.addProperty("setup_ticks", options.setupTicks());
		}
		if (!options.required()) {
			json.addProperty("required", false);
		}
		return json;
	}

	/^*
	 * Builds the {@code test_environment} that pins the transfer delay for a group of tests.
	 *
	 * @param delay transfer delay in ticks
	 * @return the entry as JSON
	 ^/
	private static JsonObject environment(int delay) {
		JsonObject rules = new JsonObject();
		rules.addProperty(TRANSFER_DELAY_RULE, delay);

		JsonObject json = new JsonObject();
		json.addProperty("type", "minecraft:game_rules");
		json.add("rules", rules);
		return json;
	}

	/^*
	 * @param delay transfer delay in ticks
	 * @return id path of the environment running tests at that delay
	 ^/
	private static String environmentName(int delay) {
		return "transfer_delay_" + delay;
	}

	@Override
	public String getName() {
		return "Fast Hoppers GameTests";
	}
}
*///?}
