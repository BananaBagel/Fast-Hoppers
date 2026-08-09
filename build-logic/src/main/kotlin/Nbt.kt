@file:Suppress("unused")

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Minimal typed NBT codec, enough to rewrite structure templates during the build.
 * <p>
 * Values keep their tag id rather than being mapped onto Kotlin types, because the difference
 * between an int and a byte is exactly what has to change when moving an item stack between
 * Minecraft versions. Anything it cannot identify makes it throw rather than guess — a build that
 * stops is far easier to diagnose than a subtly corrupt binary asset.
 */
object Nbt {
	const val END = 0
	const val BYTE = 1
	const val SHORT = 2
	const val INT = 3
	const val LONG = 4
	const val FLOAT = 5
	const val DOUBLE = 6
	const val BYTE_ARRAY = 7
	const val STRING = 8
	const val LIST = 9
	const val COMPOUND = 10
	const val INT_ARRAY = 11
	const val LONG_ARRAY = 12

	/** A tag's type id paired with its payload. */
	data class Tag(val id: Int, val value: Any)

	/** A list payload: the element type every entry shares, and the entries. */
	data class TagList(val elementType: Int, val items: MutableList<Any>)

	/** A compound payload, preserving insertion order so output stays stable. */
	class Compound(val entries: LinkedHashMap<String, Tag> = LinkedHashMap()) {
		operator fun get(key: String): Tag? = entries[key]
		operator fun set(key: String, tag: Tag) { entries[key] = tag }
		fun remove(key: String): Tag? = entries.remove(key)
		fun has(key: String): Boolean = entries.containsKey(key)
	}

	/**
	 * Reads a gzipped NBT file.
	 *
	 * @param file the file to read
	 * @return the root tag, whose payload is a [Compound] for every structure template
	 */
	fun read(file: File): Tag = DataInputStream(GZIPInputStream(file.inputStream().buffered())).use { input ->
		val rootType = input.readByte().toInt()
		input.readUTF()
		Tag(rootType, readPayload(input, rootType))
	}

	/**
	 * Writes a gzipped NBT file.
	 *
	 * @param file the file to write; parent directories are created
	 * @param root the root tag
	 */
	fun write(file: File, root: Tag) {
		file.parentFile.mkdirs()
		DataOutputStream(GZIPOutputStream(file.outputStream().buffered())).use { output ->
			output.writeByte(root.id)
			output.writeUTF("")
			writePayload(output, root.id, root.value)
		}
	}

	/**
	 * Renders a tag as SNBT, the text form 1.19.2 and earlier expect for gametest structures.
	 *
	 * @param tag the tag to render
	 * @return its SNBT representation
	 */
	fun toSnbt(tag: Tag): String = snbtPayload(tag.id, tag.value)

	private fun snbtPayload(type: Int, value: Any): String = when (type) {
		BYTE -> "${value as Byte}b"
		SHORT -> "${value as Short}s"
		INT -> "${value as Int}"
		LONG -> "${value as Long}L"
		FLOAT -> "${value as Float}f"
		DOUBLE -> "${value as Double}d"
		BYTE_ARRAY -> (value as ByteArray).joinToString(",", "[B;", "]") { "${it}b" }
		STRING -> quote(value as String)
		LIST -> (value as TagList).items.joinToString(",", "[", "]") { snbtPayload(value.elementType, it) }
		COMPOUND -> (value as Compound).entries.entries.joinToString(",", "{", "}") { (name, entry) ->
			"${quote(name)}:${snbtPayload(entry.id, entry.value)}"
		}
		INT_ARRAY -> (value as IntArray).joinToString(",", "[I;", "]")
		LONG_ARRAY -> (value as LongArray).joinToString(",", "[L;", "]") { "${it}L" }
		else -> error("Unknown NBT tag id $type")
	}

	/** Quotes and escapes a string or key. Always quoting keeps the output valid without parsing. */
	private fun quote(text: String): String =
		"\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

	private fun readPayload(input: DataInputStream, type: Int): Any = when (type) {
		BYTE -> input.readByte()
		SHORT -> input.readShort()
		INT -> input.readInt()
		LONG -> input.readLong()
		FLOAT -> input.readFloat()
		DOUBLE -> input.readDouble()
		BYTE_ARRAY -> ByteArray(input.readInt()).also(input::readFully)
		STRING -> input.readUTF()
		LIST -> {
			val elementType = input.readByte().toInt()
			val size = input.readInt()
			TagList(elementType, MutableList(size) { readPayload(input, elementType) })
		}
		COMPOUND -> {
			val compound = Compound()
			while (true) {
				val entryType = input.readByte().toInt()
				if (entryType == END) break
				compound[input.readUTF()] = Tag(entryType, readPayload(input, entryType))
			}
			compound
		}
		INT_ARRAY -> IntArray(input.readInt()) { input.readInt() }
		LONG_ARRAY -> LongArray(input.readInt()) { input.readLong() }
		else -> error("Unknown NBT tag id $type")
	}

	private fun writePayload(output: DataOutputStream, type: Int, value: Any): Unit = when (type) {
		BYTE -> output.writeByte((value as Byte).toInt())
		SHORT -> output.writeShort((value as Short).toInt())
		INT -> output.writeInt(value as Int)
		LONG -> output.writeLong(value as Long)
		FLOAT -> output.writeFloat(value as Float)
		DOUBLE -> output.writeDouble(value as Double)
		BYTE_ARRAY -> (value as ByteArray).let { output.writeInt(it.size); output.write(it) }
		STRING -> output.writeUTF(value as String)
		LIST -> (value as TagList).let { list ->
			output.writeByte(list.elementType)
			output.writeInt(list.items.size)
			list.items.forEach { writePayload(output, list.elementType, it) }
		}
		COMPOUND -> {
			(value as Compound).entries.forEach { (name, tag) ->
				output.writeByte(tag.id)
				output.writeUTF(name)
				writePayload(output, tag.id, tag.value)
			}
			output.writeByte(END)
		}
		INT_ARRAY -> (value as IntArray).let { output.writeInt(it.size); it.forEach(output::writeInt) }
		LONG_ARRAY -> (value as LongArray).let { output.writeInt(it.size); it.forEach(output::writeLong) }
		else -> error("Unknown NBT tag id $type")
	}
}
