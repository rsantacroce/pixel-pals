package app.pixelpals.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.pixelpals.model.Argb
import app.pixelpals.ui.common.KidTarget
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.add_to_my_colors
import app.pixelpals.ui.res.brightness
import app.pixelpals.ui.res.cancel
import app.pixelpals.ui.res.mix_color
import app.pixelpals.ui.res.use_color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Hue/saturation wheel plus a brightness slider. "Add to My Colors" also selects the color. */
@Composable
fun ColorMixerDialog(initial: Int, onUse: (Int) -> Unit, onAddToMyColors: (Int) -> Unit, onDismiss: () -> Unit) {
    val hsv = remember { Argb.toHsv(initial) }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(0.2f)) }
    val color = Argb.fromHsv(hue, sat, value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.mix_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HueWheel(hue, sat, value) { h, s ->
                    hue = h
                    sat = s
                }
                Text(str(Res.string.brightness), style = MaterialTheme.typography.titleSmall)
                Slider(value = value, onValueChange = { value = it }, valueRange = 0.05f..1f, modifier = Modifier.fillMaxWidth())
                CurrentColor(color, 72.dp)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    onAddToMyColors(color)
                    onDismiss()
                }, modifier = Modifier.height(KidTarget)) {
                    Text(str(Res.string.add_to_my_colors))
                }
                Button(onClick = {
                    onUse(color)
                    onDismiss()
                }, modifier = Modifier.height(KidTarget)) { Text(str(Res.string.use_color)) }
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, modifier = Modifier.height(KidTarget)) { Text(str(Res.string.cancel)) } },
    )
}

@Composable
private fun HueWheel(hue: Float, sat: Float, value: Float, onChange: (Float, Float) -> Unit) {
    fun pick(pos: Offset, w: Float) {
        val c = w / 2
        val dx = pos.x - c
        val dy = pos.y - c
        val angle = ((atan2(dy, dx) * 180 / PI).toFloat() + 360f) % 360f
        onChange(angle, (hypot(dx, dy) / c).coerceIn(0f, 1f))
    }
    val dim = Color.Black.copy(alpha = 1f - value)
    Canvas(
        Modifier
            .widthIn(max = 260.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = "Color wheel" }
            .pointerInput(Unit) { detectTapGestures { pick(it, size.width.toFloat()) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> pick(change.position, size.width.toFloat()) } },
    ) {
        val r = size.minDimension / 2
        val hues = (0..12).map { Color(Argb.fromHsv(it * 30f, 1f, 1f)) }
        drawCircle(Brush.sweepGradient(hues), r)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), radius = r), r)
        drawCircle(dim, r)
        val a = hue * PI.toFloat() / 180f
        val p = center + Offset(cos(a), sin(a)) * (sat * r)
        drawCircle(Color.White, 14f, p, style = Stroke(5f))
        drawCircle(Color.Black, 18f, p, style = Stroke(2f))
    }
}
