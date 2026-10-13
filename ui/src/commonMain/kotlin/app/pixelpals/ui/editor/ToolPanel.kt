package app.pixelpals.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.pixelpals.model.EditorAction
import app.pixelpals.model.Tool
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.Names
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.brush_size
import app.pixelpals.ui.res.filled
import app.pixelpals.ui.res.mirror_left_right
import app.pixelpals.ui.res.mirror_top_bottom
import app.pixelpals.ui.res.outline
import app.pixelpals.ui.res.tool_mirror
import app.pixelpals.ui.theme.PixelIcon

/** Tools in toolbar order. Mirror is a toggle and is inserted after the shapes. */
private val TOOLS_BEFORE_MIRROR = listOf(Tool.PENCIL, Tool.ERASER, Tool.FILL, Tool.PICKER, Tool.LINE, Tool.RECTANGLE, Tool.ELLIPSE)
private val TOOLS_AFTER_MIRROR = listOf(Tool.STAMP, Tool.MOVE, Tool.DITHER, Tool.REPLACE)

/** All tool buttons, vertical (wide layouts) or horizontal (phone bottom bar). */
@Composable
fun ToolButtons(controller: EditorController, vertical: Boolean, onOpenStamps: () -> Unit, modifier: Modifier = Modifier) {
    val s = controller.state.settings
    val mirror: @Composable () -> Unit = {
        KidButton(
            PixelIcon.MIRROR,
            str(Res.string.tool_mirror),
            onClick = { controller.dispatch(EditorAction.ToggleMirrorHorizontal) },
            selected = s.mirror.enabled,
            container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.width(76.dp),
        )
    }
    val items: List<@Composable () -> Unit> =
        TOOLS_BEFORE_MIRROR.map { t -> @Composable { ToolItem(controller, t, s.tool == t, onOpenStamps) } } +
            listOf(mirror) +
            TOOLS_AFTER_MIRROR.map { t -> @Composable { ToolItem(controller, t, s.tool == t, onOpenStamps) } }
    if (vertical) {
        // Two columns so every tool is visible without scrolling on typical desktop windows.
        Column(
            modifier.verticalScroll(rememberScrollState()).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (pair in items.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { pair.forEach { it() } }
            }
        }
    } else {
        Row(
            modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) { items.forEach { it() } }
    }
}

@Composable
private fun ToolItem(controller: EditorController, tool: Tool, selected: Boolean, onOpenStamps: () -> Unit) {
    KidButton(
        Names.toolIcon(tool),
        str(Names.tool(tool)),
        onClick = {
            if (tool == Tool.STAMP) onOpenStamps() else controller.dispatch(EditorAction.SelectTool(tool))
        },
        selected = selected,
        modifier = Modifier.width(76.dp),
    )
}

/** Contextual options for the current tool: brush size, outline/filled, mirror axes. */
@Composable
fun ToolOptions(controller: EditorController, modifier: Modifier = Modifier) {
    val s = controller.state.settings
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (s.tool.usesBrush) {
            val icons = listOf(PixelIcon.BRUSH_SMALL, PixelIcon.BRUSH_MEDIUM, PixelIcon.BRUSH_LARGE)
            for (size in 1..3) {
                KidButton(
                    icons[size - 1],
                    str(Res.string.brush_size, size),
                    onClick = { controller.dispatch(EditorAction.SetBrushSize(size)) },
                    selected = s.brushSize == size,
                    iconSize = 22.dp,
                )
            }
        }
        if (s.tool == Tool.RECTANGLE || s.tool == Tool.ELLIPSE) {
            KidButton(
                if (s.tool == Tool.RECTANGLE) PixelIcon.RECTANGLE else PixelIcon.ELLIPSE,
                str(Res.string.outline),
                onClick = { controller.dispatch(EditorAction.SetShapeFilled(false)) },
                selected = !s.shapeFilled,
                iconSize = 22.dp,
            )
            KidButton(
                PixelIcon.SHAPE_FILLED,
                str(Res.string.filled),
                onClick = { controller.dispatch(EditorAction.SetShapeFilled(true)) },
                selected = s.shapeFilled,
                iconSize = 22.dp,
            )
        }
        if (s.mirror.enabled || s.tool.usesBrush || s.tool.isShape || s.tool == Tool.FILL || s.tool == Tool.STAMP) {
            KidButton(
                PixelIcon.MIRROR,
                str(Res.string.mirror_left_right),
                onClick = { controller.dispatch(EditorAction.ToggleMirrorHorizontal) },
                selected = s.mirror.horizontal,
                iconSize = 22.dp,
                container = Color.Transparent,
            )
            KidButton(
                PixelIcon.MIRROR_V,
                str(Res.string.mirror_top_bottom),
                onClick = { controller.dispatch(EditorAction.ToggleMirrorVertical) },
                selected = s.mirror.vertical,
                iconSize = 22.dp,
            )
        }
    }
}
