package gay.bagel.fasthoppers;

import gay.bagel.fasthoppers.platform.Platform;
import gay.bagel.fasthoppers.test.FastHoppersGameTests;
import static gay.bagel.fasthoppers.FastHoppersGameRules.RegisterGameRules;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? fabric {
import gay.bagel.fasthoppers.platform.fabric.FabricPlatform;
//?} neoforge {
/*import gay.bagel.fasthoppers.platform.neoforge.NeoforgePlatform;

		*///?} forge {
/*import gay.bagel.fasthoppers.platform.forge.ForgePlatform;
 *///?}

@SuppressWarnings({"LoggingSimilarMessage", "removal"})
public class FastHoppers {

	public static final String MOD_ID = /*$ mod_id*/ "fasthoppers";
	public static final String MOD_VERSION = /*$ mod_version*/ "0.0.0";
	public static final String MOD_FRIENDLY_NAME = /*$ mod_name*/ "Fast Hoppers";
	/** The Minecraft version this jar was built for, substituted by Stonecutter. */
	public static final String MINECRAFT = /*$ minecraft*/ "1.21.1";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Platform PLATFORM = createPlatformInstance();

	public static void onInitialize() {
		LOGGER.info("Initializing {} on {} for Minecraft {}",
				MOD_ID, FastHoppers.xplat().loader(), FastHoppers.xplat().mcVersion());
		LOGGER.debug("{}: { version: {}; friendly_name: {} }", MOD_ID, MOD_VERSION, MOD_FRIENDLY_NAME);
		// NeoForge from 1.21.11 registers off RegisterEvent instead; see NeoforgeEntrypoint.
		//? fabric || forge || < 1.21.11 {
		RegisterGameRules();
		//?}
		FastHoppersGameTests.register();
	}

	public static void onInitializeClient() {
		LOGGER.info("Initializing {} Client on {}", MOD_ID, FastHoppers.xplat().loader());
		LOGGER.debug("{}: { version: {}; friendly_name: {} }", MOD_ID, MOD_VERSION, MOD_FRIENDLY_NAME);
	}

	/**
	 * The platform abstraction for the loader this build runs on.
	 *
	 * @return the active platform
	 */
	public static Platform xplat() {
		return PLATFORM;
	}

	private static Platform createPlatformInstance() {
		//? fabric {
		return new FabricPlatform();
		//?} neoforge {
		/*return new NeoforgePlatform();
		 *///?} forge {
		/*return new ForgePlatform();
		 *///?}
	}

	// "removal", not "deprecation": the constructor is marked @Deprecated(forRemoval = true) from
	// 1.20.6, and terminal deprecation is a separate warning category that "deprecation" does not
	// cover. Only reachable on 1.20.1 and earlier anyway, where it carries no deprecation at all —
	// the warning comes from the IDE resolving this shared source against a newer version's
	// classpath than the branch is written for.
	public static ResourceLocation id(String path) {
		//? > 1.20.1 {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
		 //?} <= 1.20.1 {
		/*return new ResourceLocation(MOD_ID, path);
		*///?}
	}

	/** @see #id(String) for why this is suppressed as "removal" rather than "deprecation". */
	public static ResourceLocation id(String namespace, String path) {
		//? > 1.20.1 {
		return ResourceLocation.fromNamespaceAndPath(namespace, path);
		 //?} <= 1.20.1 {
		/*return new ResourceLocation(namespace, path);
		*///?}
	}
}
