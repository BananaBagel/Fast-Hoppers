@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.loom.back.compat).apply(false)
	alias(libs.plugins.neoforged.moddev).apply(false)
	alias(libs.plugins.jsonlang.postprocess).apply(false)
	alias(libs.plugins.mod.publish.plugin).apply(false)
	alias(libs.plugins.kotlin.jvm).apply(false)
	alias(libs.plugins.devtools.ksp).apply(false)
	alias(libs.plugins.fletching.table).apply(false)
	alias(libs.plugins.legacyforge.moddev).apply(false)
}

stonecutter active file(".sc_active_version")

tasks.register("runActiveClient") {
	group = "stonecutter"
	description = "Run client of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runClient")
}

tasks.register("runActiveServer") {
	group = "stonecutter"
	description = "Run server of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runServer")
}

// Minecraft 1.21.11+ reads its tests from generated datapacks rather than building them at
// runtime, so those versions need datagen re-run whenever a test is added, renamed or retimed.
// Datagen runs on Fabric; the NeoForge buildscripts read the same output directory.
val datagenTargets = listOf("1.21.11-fabric", "26.1.2-fabric", "26.2-fabric")

// One target per distinct code path: 1.20.1-fabric is the only quick check of the structure
// downgrade, the plural resource directory and the +1 coordinate offset; 1.21.1-neoforge covers
// GameTestRegistry on a Forge-like loader; 26.2-neoforge covers the modern era end to end.
// Between them they hit both GameTest eras and every version-divergent call.
val gametestQuickTargets = listOf("1.20.1-fabric", "1.21.1-neoforge", "26.2-neoforge")

tasks.register("datagenAll") {
	group = "build"
	description = "Regenerate the test_instance and test_environment datapacks for 1.21.11+"
	dependsOn(datagenTargets.map { ":$it:runDatagen" })
}

tasks.register("gametestAll") {
	group = "verification"
	description = "Run the gametest suite on every version (run datagenAll first)"
	dependsOn(stonecutter.versions.map { ":${it.project}:runGametest" })
}

tasks.register("gametestQuick") {
	group = "verification"
	description = "Run the gametest suite on one version per code path (run datagenAll first)"
	dependsOn(gametestQuickTargets.map { ":$it:runGametest" })
}

// Each target writes its own timings into its run directory as it exits; this gathers them into
// one file so the matrix can be compared in a single sheet. Deliberately not wired to depend on
// gametestAll, so an aggregate can be taken of whatever has been run without forcing a full sweep.
tasks.register("gametestTimings") {
	group = "verification"
	description = "Collect every target's gametest timings into build/gametest-timings.csv"

	val sources = stonecutter.versions.map {
		rootProject.layout.projectDirectory.file("versions/${it.project}/run/gametest/fasthoppers-timings.csv")
	}
	val output = rootProject.layout.buildDirectory.file("gametest-timings.csv")

	inputs.files(sources).optional().withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
	outputs.file(output)

	doLast {
		val present = sources.map { it.asFile }.filter { it.isFile }
		if (present.isEmpty()) {
			error("No gametest timings found. Run gametestAll (or a single target's runGametest) first.")
		}

		// Every file repeats the header; keep the first and drop the rest.
		val rows = present.flatMap { it.readLines().drop(1) }.filter { it.isNotBlank() }
		val header = present.first().readLines().first()

		val destination = output.get().asFile
		destination.parentFile.mkdirs()
		destination.writeText((listOf(header) + rows).joinToString("\n", postfix = "\n"))

		logger.lifecycle("Collected ${rows.size} timings from ${present.size} targets into $destination")
	}
}

stonecutter parameters {
	constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge", "forge")
	swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
	swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
	swaps["mod_name"] = "\"${properties.get<String>("mod.name")}\";"
	swaps["mod_group"] = "\"${properties.get<String>("mod.group")}\";"
	swaps["minecraft"] = "\"${current.version}\";"
	constants["release"] = properties.get<String>("mod.id") != "modtemplate"
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
	group = "publishing"
	dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}
