package app.pixelpals.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import app.pixelpals.model.Palettes
import kotlin.math.sin
import kotlin.random.Random

private class Confetti(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val color: Color,
    val size: Float,
    val spin: Float,
)

/** A short, gentle confetti shower. Calls [onDone] when finished. */
@Composable
fun ConfettiOverlay(onDone: () -> Unit) {
    val pieces =
        remember {
            val rnd = Random(42)
            List(120) {
                Confetti(
                    x = rnd.nextFloat(),
                    delay = rnd.nextFloat() * 0.4f,
                    speed = 0.6f + rnd.nextFloat() * 0.6f,
                    sway = rnd.nextFloat() * 6f,
                    color = Color(Palettes.Rainbow.colors[rnd.nextInt(10)]),
                    size = 10f + rnd.nextFloat() * 14f,
                    spin = rnd.nextFloat() * 720f,
                )
            }
        }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2600, easing = LinearEasing))
        onDone()
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        for (p in pieces) {
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val y = -40f + local * p.speed * (size.height + 80f) * 1.3f
            val x = p.x * size.width + sin(local * p.sway * 3f) * 30f
            rotate(p.spin * local, Offset(x, y)) {
                drawRect(p.color.copy(alpha = 1f - local * 0.5f), Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}
