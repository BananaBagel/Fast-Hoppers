@file:Suppress("unused")

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.peanuuutz.tomlkt.Toml
import org.gradle.api.NamedDomainObjectContainer
import java.util.*

private val JSON = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }
private val TOML = Toml { }

/** Prefix ModMenu expects on every key inside its `links` object. */
private const val MODMENU_PREFIX = "modmenu."

/**
 * Builds the `custom` block of `fabric.mod.json`, carrying the ModMenu link list.
 *
 * Links come from the dedicated `mod.discord_url` / `mod.donate_url` properties plus any
 * entries in the optional `[mod.links]` table, which may add arbitrarily many more and may
 * override the dedicated ones. Keys are namespaced under `modmenu.` if they aren't already,
 * and blank URLs are dropped individually, so configuring any one link never depends on
 * another being set.
 *
 * ModMenu renders each key as a translation key, falling back to the literal key when it has
 * no translation — custom keys need an entry in the mod's own lang files to read nicely.
 *
 * @param ctx build context supplying the configured URLs
 * @return the `custom` object, or `null` when no links are configured (omitted from the manifest)
 */
private fun modMenuCustom(ctx: Context): JsonObject? {
	val links = buildMap {
		put("discord", ctx.discordUrl)
		put("donate", ctx.donateUrl)
		putAll(ctx.links)
	}.filterValues { it.isNotBlank() }
		.mapKeys { (key, _) -> if (key.startsWith(MODMENU_PREFIX)) key else "$MODMENU_PREFIX$key" }

	if (links.isEmpty()) return null

	return buildJsonObject {
		putJsonObject("modmenu") {
			putJsonObject("links") {
				links.forEach { (key, url) -> put(key, url) }
			}
		}
	}
}

sealed class Loader(val id: String) {
	abstract val modManifestPath: String
	abstract val excludedResources: List<String>

	open val isFabricLike: Boolean = false

	abstract fun generateManifest(ctx: Context): String

	object Fabric : Loader("fabric") {
		override val isFabricLike = true
		override val modManifestPath = "fabric.mod.json"
		override val excludedResources = listOf(
			"META-INF/mods.toml", "META-INF/neoforge.mods.toml", "aw/*.cfg", ".cache", "pack.mcmeta"
		)

		override fun generateManifest(ctx: Context): String {
			val manifest = FabricManifest(
				id = ctx.modId,
				name = ctx.modName,
				version = ctx.baseVersion,
				authors = ctx.authors,
				contributors = ctx.contributors,
				contact = mapOf(
					"sources" to ctx.sourcesUrl, "issues" to ctx.issuesUrl, "homepage" to ctx.homepageUrl
				),
				custom = modMenuCustom(ctx),
				description = ctx.description,
				icon = "icon.png",
				license = ctx.licenseName,
				accessWidener = "aw/${ctx.currentMcVersion}.accesswidener",
				entrypoints = mapOf(
					"main" to listOf("${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricEntrypoint"),
					"client" to listOf("${ctx.modGroup}.${ctx.modId}.platform.fabric.FabricClientEntrypoint"),
					"fabric-datagen" to listOf("${ctx.modGroup}.${ctx.modId}.platform.fabric.datagen.FabricDataGeneratorEntrypoint")
				),
				mixins = listOf("${ctx.modId}.mixins.json"),
				depends = ctx.extension.dependencies.required.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				recommends = ctx.extension.dependencies.optional.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				breaks = ctx.extension.dependencies.incompatible.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				provides = ctx.extension.dependencies.embeds.map { it.modid.get() }
			)
			return JSON.encodeToString(manifest)
		}
	}

	sealed class ForgeLike(id: String) : Loader(id) {
		override val excludedResources = listOf(
			"fabric.mod.json", "aw/*.accesswidener", ".cache"
		)

		override fun generateManifest(ctx: Context): String {
			val forgeDeps = mutableListOf<ForgeDependency>()

			fun addDeps(container: NamedDomainObjectContainer<Dependency>, type: String) {
				container.forEach {
					forgeDeps.add(
						ForgeDependency(
							modId = it.modid.get(),
							side = it.environment.get().uppercase(Locale.getDefault()),
							versionRange = it.forgeLikeVersionRange.get(),
							mandatory = type == "required",
							type = type
						)
					)
				}
			}

			addDeps(ctx.extension.dependencies.required, "required")
			addDeps(ctx.extension.dependencies.optional, "optional")
			addDeps(ctx.extension.dependencies.incompatible, "incompatible")

			val manifest = ForgeManifest(
				license = ctx.licenseName,
				issueTrackerURL = ctx.issuesUrl,
				mods = listOf(
					ForgeMod(
						modId = ctx.modId,
						displayName = ctx.modName,
						version = ctx.baseVersion,
						displayURL = ctx.homepageUrl,
						modUrl = ctx.homepageUrl,
						// Must stay a bare filename: Forge <1.20 reads it via
						// AbstractPackResources.getRootResource(String), which rejects any '/'.
						logoFile = "icon.png",
						authors = ctx.authors.joinToString(", "),
						credits = "${ctx.authors.joinToString(", ")}${if (ctx.contributors.isNotEmpty()) " Contributors: ${ctx.contributors.joinToString(", ")}" else ""}",
						description = ctx.description
					)
				),
				dependencies = mapOf(ctx.modId to forgeDeps),
				mixins = listOf(ForgeMixin("${ctx.modId}.mixins.json")),
				accessTransformers = listOf(ForgeAccessTransformer("aw/${ctx.stonecutter.current.version}.cfg"))
			)

			return TOML.encodeToString(manifest)
		}
	}

	object NeoForge : ForgeLike("neoforge") {
		override val modManifestPath = "META-INF/neoforge.mods.toml"
		override val excludedResources = (super.excludedResources + "META-INF/mods.toml") + "pack.mcmeta"
	}

	object Forge : ForgeLike("forge") {
		override val modManifestPath = "META-INF/mods.toml"
		override val excludedResources = super.excludedResources + "META-INF/neoforge.mods.toml"
		val mixinConfigAttribute = "MixinConfigs"
	}

	companion object {
		fun of(id: String): Loader = when (id) {
			"fabric" -> Fabric
			"neoforge" -> NeoForge
			"forge" -> Forge
			else -> error("Unknown loader: '$id'")
		}
	}
}
