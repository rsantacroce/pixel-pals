package app.pixelpals.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pixelpals.model.EditorAction
import app.pixelpals.model.Palettes
import app.pixelpals.ui.common.ColorSwatch
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.Names
import app.pixelpals.ui.common.SectionTitle
import app.pixelpals.ui.common.colorName
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.color_swatch
import app.pixelpals.ui.res.current_color
import app.pixelpals.ui.res.mix_color
import app.pixelpals.ui.res.my_colors
import app.pixelpals.ui.res.palette_challenge
import app.pixelpals.ui.res.recent_colors
import app.pixelpals.ui.theme.PixelIcon

/** Colors visible in the main swatch row: the challenge colors or the chosen palette. */
fun EditorController.activePalette(): List<Int> = challenge?.palette ?: Palettes.byId(project.paletteId).colors

/** Big swatch showing the current color. */
@Composable
fun CurrentColor(color: Int, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val desc = str(Res.string.current_color) + " " + colorName(color)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(color))
            .border(3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc },
    )
}

/** Compact palette row for phones: current color, palette swatches, my colors and recent colors in one scroll. */
@Composable
fun PaletteStrip(
    controller: EditorController,
    customColors: List<Int>,
    onMix: () -> Unit,
    onPalettes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = controller.state
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.padding(end = 6.dp)) { CurrentColor(state.color) }
        KidButton(PixelIcon.PALETTE, str(Names.palette(controller.project.paletteId)), onClick = onPalettes)
        val colors = (state.recentColors + controller.activePalette() + customColors).distinct()
        for (c in colors) Swatch(controller, c, 40.dp)
        KidButton(PixelIcon.COLOR_WHEEL, str(Res.string.mix_color), onClick = onMix)
    }
}

/** Full color panel for wide layouts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPanel(controller: EditorController, customColors: List<Int>, onMix: () -> Unit, modifier: Modifier = Modifier) {
    val state = controller.state
    Column(modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CurrentColor(state.color, 64.dp)
            KidButton(PixelIcon.COLOR_WHEEL, str(Res.string.mix_color), onClick = onMix, modifier = Modifier.padding(start = 8.dp))
        }
        if (controller.challenge != null) {
            SectionTitle(str(Res.string.palette_challenge))
        } else {
            PaletteChooser(controller)
        }
        FlowRow { for (c in controller.activePalette()) Swatch(controller, c, 36.dp) }
        if (customColors.isNotEmpty()) {
            SectionTitle(str(Res.string.my_colors))
            FlowRow { for (c in customColors) Swatch(controller, c, 36.dp) }
        }
        if (state.recentColors.isNotEmpty()) {
            SectionTitle(str(Res.string.recent_colors))
            FlowRow { for (c in state.recentColors) Swatch(controller, c, 36.dp) }
        }
    }
}

/** Chips to switch between the built-in palettes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaletteChooser(controller: EditorController, onPicked: () -> Unit = {}) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (p in Palettes.all) {
            FilterChip(
                selected = controller.project.paletteId == p.id,
                onClick = {
                    controller.setPalette(p.id)
                    onPicked()
                },
                label = { Text(str(Names.palette(p.id))) },
                leadingIcon = {
                    Row { p.colors.take(4).forEach { Box(Modifier.size(8.dp).background(Color(it))) } }
                },
            )
        }
    }
}

@Composable
private fun Swatch(controller: EditorController, color: Int, size: Dp) {
    ColorSwatch(
        color = color,
        selected = controller.state.color == color,
        onClick = { controller.dispatch(EditorAction.SetColor(color)) },
        size = size,
        description = str(Res.string.color_swatch, colorName(color)),
    )
}
