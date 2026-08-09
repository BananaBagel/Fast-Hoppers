import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Rewrites gametest structure templates so one set of source files works on every Minecraft
 * version.
 *
 * Structures are authored once, in whatever format the game wrote them, and Minecraft's data
 * fixers only ever convert *forward*. A template saved in 1.21.1 therefore loads on 1.21.11 and
 * later untouched, but on 1.20.1 and earlier the blocks place while every container comes out
 * empty — the item stack format changed in 1.20.5, and there is no fixer going the other way.
 *
 * Rather than keep a second copy of every structure, or make authors remember to convert by hand
 * whenever they rebuild one in game, this lowers the copy that goes into older jars:
 *
 *  - `DataVersion` is set to [dataVersion], so the game applies its own fixers from there forward.
 *  - Item stack counts move from `count` (int, 1.20.5 and later) back to `Count` (byte).
 *  - `components` is dropped, having no meaning before 1.20.5.
 *
 * Output also lands under `structures/` rather than `structure/`, which is where versions up to
 * 1.20.1 look for it.
 */
abstract class DowngradeStructuresTask : DefaultTask() {

	/** Resource root holding the authored templates, at `data/<namespace>/structure/`. */
	@get:InputDirectory
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val sourceRoot: DirectoryProperty

	/** Where the rewritten templates are written, mirroring the resource layout. */
	@get:OutputDirectory
	abstract val outputRoot: DirectoryProperty

	/** Data version to stamp on the output; the oldest version the mod supports. */
	@get:Input
	abstract val dataVersion: Property<Int>

	/**
	 * Whether to emit SNBT under `gametest/structures/` instead of gzipped NBT under `structures/`.
	 *
	 * Vanilla 1.19.2 loads gametest templates as text from that path; 1.20.1 moved to binary
	 * templates alongside ordinary structures. Forge patches 1.19.2 to accept the newer layout, so
	 * this only matters for Fabric down there — but emitting both costs nothing and keeps the rule
	 * about the Minecraft version rather than the loader.
	 */
	@get:Input
	abstract val emitSnbt: Property<Boolean>

	@TaskAction
	fun downgrade() {
		val source = sourceRoot.get().asFile
		val output = outputRoot.get().asFile
		output.deleteRecursively()

		val templates = source.resolve("data").listFiles().orEmpty()
			.filter(File::isDirectory)
			.flatMap { namespace ->
				namespace.resolve("structure").listFiles().orEmpty()
					.filter { it.isFile && it.extension == "nbt" }
					.map { namespace.name to it }
			}

		templates.forEach { (namespace, file) ->
			val root = Nbt.read(file)
			val compound = root.value as? Nbt.Compound
				?: error("${file.name} is not a structure template: root tag is not a compound")

			compound["DataVersion"] = Nbt.Tag(Nbt.INT, dataVersion.get())
			lowerItemStacks(compound)

			// "structures", plural, is where 1.20.1 and earlier look.
			Nbt.write(output.resolve("data/$namespace/structures/${file.name}"), root)

			if (emitSnbt.get()) {
				val name = file.nameWithoutExtension
				output.resolve("data/$namespace/gametest/structures/$name.snbt").apply {
					parentFile.mkdirs()
					writeText(Nbt.toSnbt(root))
				}
			}
		}

		logger.lifecycle("Lowered ${templates.size} structure templates to data version ${dataVersion.get()}")
	}

	/**
	 * Walks a template's block entities *and* its entities, rewriting every item stack found.
	 *
	 * Entities matter as much as blocks here. A hopper minecart keeps its inventory in the
	 * template's `entities` list rather than `blocks`, so a template containing one would
	 * otherwise place the cart perfectly and hand it over empty on older versions — the same
	 * silent failure containers had before any of this existed.
	 *
	 * @param structure the template's root compound
	 */
	private fun lowerItemStacks(structure: Nbt.Compound) {
		listOf("blocks", "entities")
			.mapNotNull { structure[it]?.value as? Nbt.TagList }
			.flatMap { it.items.filterIsInstance<Nbt.Compound>() }
			.mapNotNull { it["nbt"]?.value as? Nbt.Compound }
			.forEach(::lowerHolder)
	}

	/**
	 * Rewrites every item stack held directly by one block entity or entity.
	 *
	 * @param holder the block entity or entity data, as stored under its `nbt` key
	 */
	private fun lowerHolder(holder: Nbt.Compound) {
		// Containers, including hopper minecarts.
		(holder["Items"]?.value as? Nbt.TagList)
			?.items
			?.filterIsInstance<Nbt.Compound>()
			?.forEach(::lowerStack)

		// A dropped item entity carries a single stack instead.
		(holder["Item"]?.value as? Nbt.Compound)?.let(::lowerStack)
	}

	/**
	 * Moves one item stack back to the pre-1.20.5 format, in place.
	 *
	 * @param stack the stack compound to rewrite
	 */
	private fun lowerStack(stack: Nbt.Compound) {
		stack.remove("count")?.let { count ->
			val size = count.value as? Int
				?: error("item stack count was tag ${count.id}, expected an int")
			stack["Count"] = Nbt.Tag(Nbt.BYTE, size.toByte())
		}
		stack.remove("components")
	}
}
