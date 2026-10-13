package app.pixelpals.io

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Upgrades older `.pixelpals` JSON to [CURRENT_VERSION], one version at a time.
 * To change the format: bump [CURRENT_VERSION] and add a `vN -> vN+1` step to [steps].
 */
public object ProjectMigrations {
    /** Version written by this build. */
    public const val CURRENT_VERSION: Int = 2

    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf(1 to ::v1ToV2)

    /** Returns [json] migrated to the current version. Throws [IllegalArgumentException] for unknown/newer versions. */
    public fun migrate(json: JsonObject): JsonObject {
        var doc = json
        var version = doc["formatVersion"]?.jsonPrimitive?.int ?: 1
        require(version <= CURRENT_VERSION) { "File was made by a newer version of the app ($version)" }
        while (version < CURRENT_VERSION) {
            val step = steps[version] ?: throw IllegalArgumentException("No migration from version $version")
            doc = step(doc)
            version++
        }
        return doc
    }

    /**
     * v1 (pre-animation) stored pixels directly on each layer and had no frames, fps, nextId or locked layers.
     * v2 moves those pixels into a single frame.
     */
    private fun v1ToV2(v1: JsonObject): JsonObject {
        val layers = v1.getValue("layers").jsonArray.map { it.jsonObject }
        val maxId = layers.maxOfOrNull { it.getValue("id").jsonPrimitive.int } ?: 0
        val frameId = maxId + 1
        val cels = buildJsonObject { layers.forEach { put(it.getValue("id").jsonPrimitive.content, it.getValue("pixels")) } }
        return JsonObject(
            v1 +
                mapOf(
                    "formatVersion" to JsonPrimitive(2),
                    "fps" to JsonPrimitive(app.pixelpals.model.Drawing.DEFAULT_FPS),
                    "nextId" to JsonPrimitive(frameId + 1),
                    "layers" to JsonArray(layers.map { JsonObject(it - "pixels") }),
                    "frames" to
                        JsonArray(
                            listOf(
                                buildJsonObject {
                                    put("id", frameId)
                                    put("cels", cels)
                                },
                            ),
                        ),
                ),
        )
    }
}
