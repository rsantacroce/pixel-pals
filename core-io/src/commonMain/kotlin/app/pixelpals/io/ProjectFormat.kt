package app.pixelpals.io

import kotlinx.serialization.Serializable

/** On-disk JSON schema of a `.pixelpals` file. See docs/ARCHITECTURE.md for the full spec. */
@Serializable
internal data class ProjectFileDto(
    val formatVersion: Int,
    val app: String = "PixelPals",
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val width: Int,
    val height: Int,
    val fps: Int,
    val nextId: Int,
    val paletteId: String = "bright",
    val templateId: String? = null,
    val challengeId: String? = null,
    val layers: List<LayerDto>,
    val frames: List<FrameDto>,
    val thumbnail: ThumbnailDto,
)

@Serializable
internal data class LayerDto(val id: Int, val name: String, val visible: Boolean = true, val locked: Boolean = false)

/** [cels] maps a layer id (as a string) to RLE+Base64 pixels, see [PixelCodec]. */
@Serializable
internal data class FrameDto(val id: Int, val cels: Map<String, String>)

@Serializable
internal data class ThumbnailDto(val width: Int, val height: Int, val pixels: String)
