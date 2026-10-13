package app.pixelpals.ui

/** App screens. Encoded as a string so the current screen survives Android process death. */
sealed interface Route {
    data object Gallery : Route

    data class Editor(val projectId: String) : Route

    data object RecycleBin : Route

    data object ParentSettings : Route

    fun encode(): String =
        when (this) {
            Gallery -> "gallery"
            is Editor -> "editor:$projectId"
            RecycleBin -> "bin"
            ParentSettings -> "parent"
        }

    companion object {
        fun decode(s: String): Route =
            when {
                s.startsWith("editor:") -> Editor(s.removePrefix("editor:"))
                s == "bin" -> RecycleBin
                // Never restore straight into parent settings without the gate.
                else -> Gallery
            }
    }
}
