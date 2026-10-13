package app.pixelpals.model

/** A small built-in picture that can be placed with the Stamp tool. */
public class Stamp(public val id: String, public val pixels: PixelBuffer)

/** Original pixel stamps, authored here as character maps. */
public object Stamps {
    private val colorKey: Map<Char, Int> =
        mapOf(
            'k' to 0x222034,
            'w' to 0xFFFFFF,
            'r' to 0xE8263B,
            'p' to 0xFF77A8,
            'y' to 0xFFD93D,
            'o' to 0xFF8C1A,
            'g' to 0x3EC74B,
            'G' to 0x1E7A3A,
            'b' to 0x3A7BFF,
            's' to 0xB8B8C4,
            'n' to 0x8B5A2B,
        ).mapValues { Argb.rgb(it.value) }

    /** Parses rows of characters into a buffer; '.' and unknown characters are transparent. */
    public fun parse(rows: List<String>, key: Map<Char, Int> = colorKey): PixelBuffer {
        val w = rows.maxOf { it.length }
        val h = rows.size
        val px = IntArray(w * h)
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, ch -> px[y * w + x] = key[ch] ?: Argb.TRANSPARENT } }
        return PixelBuffer.wrap(w, h, px)
    }

    private fun stamp(id: String, vararg rows: String) = Stamp(id, parse(rows.toList()))

    public val all: List<Stamp> =
        listOf(
            stamp(
                "heart",
                ".kkk.kkk.",
                "krrrkrrrk",
                "krwrrrrrk",
                "krrrrrrrk",
                ".krrrrrk.",
                "..krrrk..",
                "...krk...",
                "....k....",
            ),
            stamp(
                "star",
                "....k....",
                "...kyk...",
                "kkkyyykkk",
                "kyyyyyyyk",
                ".kyyyyyk.",
                "..kyyyk..",
                ".kyykyyk.",
                ".kyk.kyk.",
                ".kk...kk.",
            ),
            stamp(
                "smiley",
                "..kkkkk..",
                ".kyyyyyk.",
                "kyykykyyk",
                "kyykykyyk",
                "kyyyyyyyk",
                "kykyyykyk",
                "kyykkkyyk",
                ".kyyyyyk.",
                "..kkkkk..",
            ),
            stamp(
                "flower",
                "..pp.pp..",
                ".ppppppp.",
                ".ppyyypp.",
                "..pyyyp..",
                ".ppyyypp.",
                ".ppppppp.",
                "..pp.pp..",
                "....g....",
                ".gg.g.gg.",
                "..ggggg..",
                "....g....",
            ),
            stamp(
                "cat",
                "k.........k",
                "kk.......kk",
                "kpkkkkkkkpk",
                "koooooooook",
                "kookoookook",
                "kookoookook",
                "koooopooook",
                "koookokoook",
                ".koooooook.",
                "..kkkkkkk..",
            ),
            stamp(
                "sword",
                "...k...",
                "..kwk..",
                "..ksk..",
                "..ksk..",
                "..ksk..",
                "..ksk..",
                "..ksk..",
                "kkkkkkk",
                "kyyyyyk",
                "kkknkkk",
                "..knk..",
                "..knk..",
                "..kyk..",
                "...k...",
            ),
            stamp(
                "mushroom",
                "...kkkk...",
                "..krrwrk..",
                ".krwrrrrk.",
                "krrrrrwrrk",
                "krwrrrrrrk",
                ".kkkkkkkk.",
                "..kwwwwk..",
                "..kwwwwk..",
                "..kkkkkk..",
            ),
            stamp(
                "rocket",
                "...k...",
                "..krk..",
                ".krrrk.",
                ".kwwwk.",
                ".kwbwk.",
                ".kwwwk.",
                ".kwwwk.",
                "kkwwwkk",
                "krkkkrk",
                "kk.o.kk",
                "..oyo..",
                "...o...",
            ),
            stamp(
                "ghost",
                "..kkkkk..",
                ".kwwwwwk.",
                "kwwwwwwwk",
                "kwkwwwkwk",
                "kwkwwwkwk",
                "kwwwwwwwk",
                "kwwwkwwwk",
                "kwwwwwwwk",
                "kk.kkk.kk",
            ),
            stamp(
                "tree",
                "...ggg...",
                "..gggGg..",
                ".gGggggg.",
                "gggggGggg",
                ".ggGgggg.",
                "..ggggg..",
                "....n....",
                "....n....",
                "...nnn...",
            ),
        )

    /** Looks up a stamp by id. */
    public fun byId(id: String): Stamp? = all.firstOrNull { it.id == id }
}
