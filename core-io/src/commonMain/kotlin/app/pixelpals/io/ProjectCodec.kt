package app.pixelpals.io

import app.pixelpals.model.Drawing
import app.pixelpals.model.Frame
import app.pixelpals.model.LayerInfo
import app.pixelpals.model.PixelBuffer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** Converts [Project]s to and from `.pixelpals` JSON. */
public object ProjectCodec {
    /** File extension, without the dot. */
    public const val EXTENSION: String = "pixelpals"

    /** Longest edge of the embedded thumbnail. */
    public const val THUMBNAIL_MAX: Int = 64

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /** Serializes [project] to JSON text. */
    public fun encode(project: Project): String {
        val d = project.drawing
        val thumb = thumbnail(d)
        val dto =
            ProjectFileDto(
                formatVersion = ProjectMigrations.CURRENT_VERSION,
                id = project.id,
                name = project.name,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
                width = d.width,
                height = d.height,
                fps = d.fps,
                nextId = d.nextId,
                paletteId = project.paletteId,
                templateId = project.templateId,
                challengeId = project.challengeId,
                layers = d.layers.map { LayerDto(it.id, it.name, it.visible, it.locked) },
                frames =
                    d.frames.map { f ->
                        FrameDto(f.id, d.layers.associate { it.id.toString() to PixelCodec.encode(f.cels.getValue(it.id)) })
                    },
                thumbnail = ThumbnailDto(thumb.width, thumb.height, PixelCodec.encode(thumb)),
            )
        return json.encodeToString(ProjectFileDto.serializer(), dto)
    }

    /** Parses JSON text, migrating old versions. Throws [CorruptProjectException] for anything unreadable. */
    public fun decode(text: String): Project =
        guard {
            val dto = parse(text)
            val layers = dto.layers.map { LayerInfo(it.id, it.name, it.visible, it.locked) }
            require(layers.map { it.id }.toSet().size == layers.size) { "Duplicate layer ids" }
            val count = dto.width * dto.height
            val frames =
                dto.frames.map { f ->
                    Frame(
                        f.id,
                        layers.associate { l ->
                            val text = f.cels[l.id.toString()] ?: throw IllegalArgumentException("Missing pixels for layer ${l.id}")
                            l.id to PixelBuffer.wrap(dto.width, dto.height, PixelCodec.decode(text, count))
                        },
                    )
                }
            val maxId = (layers.map { it.id } + frames.map { it.id }).max()
            val drawing = Drawing(dto.width, dto.height, layers, frames, dto.fps.coerceIn(1, 24), maxOf(dto.nextId, maxId + 1))
            Project(dto.id, dto.name, dto.createdAt, dto.updatedAt, drawing, dto.paletteId, dto.templateId, dto.challengeId)
        }

    /** Reads only what the gallery needs (skips decoding layer pixels). */
    public fun decodeSummary(text: String): ProjectSummary =
        guard {
            val dto = parse(text)
            val t = dto.thumbnail
            ProjectSummary(
                dto.id,
                dto.name,
                dto.updatedAt,
                dto.width,
                dto.height,
                dto.frames.size,
                PixelBuffer.wrap(t.width, t.height, PixelCodec.decode(t.pixels, t.width * t.height)),
            )
        }

    private fun parse(text: String): ProjectFileDto {
        val migrated = ProjectMigrations.migrate(json.parseToJsonElement(text).jsonObject)
        val dto = json.decodeFromJsonElement(ProjectFileDto.serializer(), migrated)
        require(dto.width in 1..Drawing.MAX_SIZE && dto.height in 1..Drawing.MAX_SIZE) { "Bad size" }
        require(dto.layers.isNotEmpty() && dto.frames.isNotEmpty()) { "Empty drawing" }
        return dto
    }

    private inline fun <T> guard(block: () -> T): T =
        try {
            block()
        } catch (e: CorruptProjectException) {
            throw e
        } catch (e: Exception) {
            throw CorruptProjectException(e.message ?: "Unreadable drawing", e)
        }

    /** First-frame composite, downscaled (nearest neighbor) so its longest edge is at most [THUMBNAIL_MAX]. */
    public fun thumbnail(d: Drawing): PixelBuffer {
        val full = d.composite(0)
        val step = (maxOf(d.width, d.height) + THUMBNAIL_MAX - 1) / THUMBNAIL_MAX
        if (step <= 1) return PixelBuffer.wrap(d.width, d.height, full)
        val w = (d.width + step - 1) / step
        val h = (d.height + step - 1) / step
        val out = IntArray(w * h) { i -> full[(i / w * step) * d.width + (i % w) * step] }
        return PixelBuffer.wrap(w, h, out)
    }
}
