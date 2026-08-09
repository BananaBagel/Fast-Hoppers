plugins {
	id("mod-platform")
	id("dev.kikugie.loom-back-compat")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	replacements.string(current.parsed >= "1.21.11") {
		replace("ResourceLocation", "Identifier")
		replace("location()", "identifier()")
	}
	replacements.string(current.parsed >= "26.1.2") {
		replace("FabricDataOutput", "FabricPackOutput")
	}
}

platform {
	loader = "fabric"
	dependencies {
		required("minecraft") {
			fabricLikeVersionRange = prop("deps.minecraft")
		}
		if (hasProperty("deps.fabric-api")) {
			required("fabric-api") {
				slug("fabric-api")
				fabricLikeVersionRange = ">=${prop("deps.fabric-api")}"
			}
		}
		if (!hasProperty("deps.fabric-api")) {
			required("fabric-api") {
				slug("fabric-api")
				fabricLikeVersionRange = ">=${prop("deps.fabric-api-dev")}"
			}
		}
		required("fabricloader") {
			fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
		}
		optional("modmenu") {}
	}
}

loom {
	accessWidenerPath = rootProject.file("src/main/resources/aw/${sc.current.version}.accesswidener")
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=GayBagel")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}
	// Headless GameTest run. fabric-api's gametest module reacts to -Dfabric-api.gametest by
	// booting a GameTestServer instead of a normal server, running every test registered with
	// vanilla's GameTestRegistry, then exiting with a non-zero code if any failed.
	runs.register("gametest") {
		server()
		ideConfigGenerated(true)
		runDir = "run/gametest"
		environment = "server"
		configName = "Fabric Game Test"
		vmArgs(
			"-Dfabric-api.gametest",
			"-Dfabric-api.gametest.report-file=${layout.buildDirectory.get().asFile}/gametest-results.xml"
		)
	}
}

fabricApi {
	configureDataGeneration {
		outputDirectory = file("${rootDir}/versions/datagen/${sc.current.version.split("-")[0]}/src/main/generated")
		client = true
	}
}

repositories {
	mavenCentral()
	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
}

configurations.all {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	if (sc.current.parsed < "26") {
		mappings(loom.layered {
			officialMojangMappings()
			if (hasProperty("deps.parchment"))
				parchment("org.parchmentmc.data:parchment-${prop("deps.parchment")}@zip")
		})
	}
	modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
// implementation(libs.moulberry.mixinconstraints)
// include(libs.moulberry.mixinconstraints)
	if (hasProperty("deps.fabric-api")) {
		modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
	}
	modLocalRuntime("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
	if (hasProperty("deps.fabric-api-dev") && !hasProperty("deps.fabric-api")) {
		modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api-dev")}")
	}
	// What actually implements -Dfabric-api.gametest, and the reason the headless runner exists at
	// all: it swaps the server for a GameTestServer, runs everything in GameTestRegistry, then exits
	// non-zero on failure. The module is dev-only, so it is deliberately left out of the aggregate
	// fabric-api jar and has to be asked for by name — without it the property means nothing to
	// anyone and the run boots an ordinary server that never exits. Runtime-only, so it stays out of
	// published jars.
	modLocalRuntime(
		fabricApi.module(
			"fabric-gametest-api-v1",
			prop(if (hasProperty("deps.fabric-api")) "deps.fabric-api" else "deps.fabric-api-dev")
		)
	)
}
