package app.pixelpals.model

/** A named list of colors. [id] is stable and used to look up a localized display name. */
public data class Palette(val id: String, val colors: List<Int>)

/** Built-in palettes. All color selections are original to this app. */
public object Palettes {
    private fun of(id: String, vararg rgb: Int) = Palette(id, rgb.map { Argb.rgb(it) })

    /** The default kid-friendly palette: bright hues plus black, white and greys. */
    public val Bright: Palette =
        of(
            "bright",
            0xE8263B,
            0xFF6B6B,
            0xFF8C1A,
            0xFFB347,
            0xFFD93D,
            0xFFF27A,
            0x3EC74B,
            0x9BE15D,
            0x1E7A3A,
            0x2ED1C4,
            0x3A7BFF,
            0x8FC7FF,
            0x1B3DB5,
            0x8E44FF,
            0xC69CFF,
            0xFF4FB4,
            0xFFA8D9,
            0x8B5A2B,
            0xD9A066,
            0xFFD6B0,
            0x222034,
            0x6B6B78,
            0xB8B8C4,
            0xFFFFFF,
        )

    /** Rainbow order, bright and saturated. */
    public val Rainbow: Palette =
        of(
            "rainbow",
            0xFF1E3C,
            0xFF7A00,
            0xFFD000,
            0x7ED321,
            0x00B86B,
            0x00C2E0,
            0x2D6BFF,
            0x6A3DFF,
            0xB03DFF,
            0xFF3DAE,
            0x222034,
            0xFFFFFF,
        )

    /** Soft pastel tones. */
    public val Pastel: Palette =
        of(
            "pastel",
            0xFFB3BA,
            0xFFDFBA,
            0xFFFFBA,
            0xBAFFC9,
            0xBAE1FF,
            0xD7BAFF,
            0xFFC8E8,
            0xC9F2E7,
            0xF5E6CC,
            0xE0E0F0,
            0x6E6A80,
            0xFFFFFF,
        )

    /** Greens and earthy browns. */
    public val Forest: Palette =
        of(
            "forest",
            0x0F3D24,
            0x1E6B3A,
            0x3E9E4F,
            0x7CC36A,
            0xC4E38A,
            0x5C3A1E,
            0x8B5A2B,
            0xC28A4E,
            0xE8C27A,
            0xD94F3D,
            0xF2E9D8,
            0x1C1C22,
        )

    /** Blues, teals and sandy colors. */
    public val Ocean: Palette =
        of(
            "ocean",
            0x0A1F44,
            0x123E7C,
            0x1F6FC2,
            0x3FA7F0,
            0x8ED8FF,
            0x0FA3A3,
            0x5FE3D0,
            0xF2D9A0,
            0xFF8A65,
            0xFFFFFF,
            0x6C7A89,
            0x1A1A24,
        )

    /** A chunky retro-game feel with a limited set of punchy colors (original selection). */
    public val Retro: Palette =
        of(
            "retro",
            0x000000,
            0x1D2B53,
            0x7E2553,
            0x00874E,
            0xA8572F,
            0x5A5550,
            0xC0C2C8,
            0xFFF4E6,
            0xF0004D,
            0xFFA000,
            0xFFEC26,
            0x00E43C,
            0x2BADFF,
            0x83769C,
            0xFF77A8,
            0xFFCCAA,
        )

    /** Every built-in palette in display order. */
    public val all: List<Palette> = listOf(Bright, Rainbow, Pastel, Forest, Ocean, Retro)

    /** Looks up a palette by id, falling back to [Bright]. */
    public fun byId(id: String): Palette = all.firstOrNull { it.id == id } ?: Bright
}

/** Pure helpers for the "My Colors" and "Recently used" rows. */
public object ColorMemory {
    /** Maximum number of custom colors kept in "My Colors". */
    public const val MAX_CUSTOM: Int = 16

    /** Maximum number of recently used colors. */
    public const val MAX_RECENT: Int = 8

    /** Adds [color] to the end of [list] unless already present; drops the oldest beyond [max]. */
    public fun addCustom(list: List<Int>, color: Int, max: Int = MAX_CUSTOM): List<Int> =
        if (color in list) list else (list + color).takeLast(max)

    /** Moves [color] to the front of [list], keeping at most [max] entries. */
    public fun pushRecent(list: List<Int>, color: Int, max: Int = MAX_RECENT): List<Int> =
        (listOf(color) + list.filter { it != color }).take(max)
}
