package app.pixelpals.ui.platform

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates every app sound procedurally as 16-bit mono PCM, so there are no audio asset files
 * and no licensing questions. All sounds are short and soft.
 */
object SoundSynth {
    const val SAMPLE_RATE: Int = 22050

    /** PCM samples for [sound]. */
    fun samples(sound: Sound): ShortArray =
        when (sound) {
            Sound.POP -> tone(0.06, 0.35) { t -> 900.0 - 5000.0 * t }
            Sound.CLICK -> tone(0.03, 0.25) { 1400.0 }
            Sound.SPLASH -> noise(0.28, 0.30, decay = 12.0, seed = 1, smooth = 0.55)
            Sound.WHOOSH -> noise(0.22, 0.25, decay = 6.0, seed = 2, smooth = 0.85, swell = true)
            Sound.WIPE -> tone(0.35, 0.3) { t -> 800.0 - 1600.0 * t }
            Sound.TADA -> arpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 0.11, 0.3)
        }

    private fun tone(seconds: Double, volume: Double, freq: (Double) -> Double): ShortArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            phase += 2 * PI * freq(t) / SAMPLE_RATE
            val env = envelope(i, n)
            (sin(phase) * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun noise(seconds: Double, volume: Double, decay: Double, seed: Int, smooth: Double, swell: Boolean = false): ShortArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val rnd = Random(seed)
        var last = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / n
            last = last * smooth + (rnd.nextDouble() * 2 - 1) * (1 - smooth)
            val env = if (swell) sin(PI * t) else exp(-decay * t)
            (last * env * volume * 3 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun arpeggio(notes: List<Double>, noteSeconds: Double, volume: Double): ShortArray {
        val parts = notes.map { f -> tone(noteSeconds * 1.6, volume) { f } }
        val step = (noteSeconds * SAMPLE_RATE).toInt()
        val out = IntArray(step * (notes.size - 1) + parts.last().size)
        parts.forEachIndexed { k, p -> p.forEachIndexed { i, s -> out[k * step + i] += s.toInt() } }
        return ShortArray(out.size) { out[it].coerceIn(-32767, 32767).toShort() }
    }

    private fun envelope(i: Int, n: Int): Double {
        val attack = minOf(1.0, i / (0.005 * SAMPLE_RATE))
        return attack * exp(-4.0 * i / n)
    }
}
