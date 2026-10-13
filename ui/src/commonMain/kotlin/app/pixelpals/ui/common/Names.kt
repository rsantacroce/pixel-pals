package app.pixelpals.ui.common

import androidx.compose.runtime.Composable
import app.pixelpals.model.Tool
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.palette_bright
import app.pixelpals.ui.res.palette_forest
import app.pixelpals.ui.res.palette_ocean
import app.pixelpals.ui.res.palette_pastel
import app.pixelpals.ui.res.palette_rainbow
import app.pixelpals.ui.res.palette_retro
import app.pixelpals.ui.res.stamp_cat
import app.pixelpals.ui.res.stamp_flower
import app.pixelpals.ui.res.stamp_ghost
import app.pixelpals.ui.res.stamp_heart
import app.pixelpals.ui.res.stamp_mushroom
import app.pixelpals.ui.res.stamp_rocket
import app.pixelpals.ui.res.stamp_smiley
import app.pixelpals.ui.res.stamp_star
import app.pixelpals.ui.res.stamp_sword
import app.pixelpals.ui.res.stamp_tree
import app.pixelpals.ui.res.tool_dither
import app.pixelpals.ui.res.tool_ellipse
import app.pixelpals.ui.res.tool_eraser
import app.pixelpals.ui.res.tool_fill
import app.pixelpals.ui.res.tool_line
import app.pixelpals.ui.res.tool_move
import app.pixelpals.ui.res.tool_pencil
import app.pixelpals.ui.res.tool_picker
import app.pixelpals.ui.res.tool_rectangle
import app.pixelpals.ui.res.tool_replace
import app.pixelpals.ui.res.tool_stamp
import app.pixelpals.ui.res.tpl_dinosaur
import app.pixelpals.ui.res.tpl_fish
import app.pixelpals.ui.res.tpl_flower
import app.pixelpals.ui.res.tpl_house
import app.pixelpals.ui.res.tpl_robot
import app.pixelpals.ui.theme.PixelIcon
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Localized labels for model ids. */
object Names {
    fun tool(tool: Tool): StringResource =
        when (tool) {
            Tool.PENCIL -> Res.string.tool_pencil
            Tool.ERASER -> Res.string.tool_eraser
            Tool.FILL -> Res.string.tool_fill
            Tool.PICKER -> Res.string.tool_picker
            Tool.LINE -> Res.string.tool_line
            Tool.RECTANGLE -> Res.string.tool_rectangle
            Tool.ELLIPSE -> Res.string.tool_ellipse
            Tool.MOVE -> Res.string.tool_move
            Tool.STAMP -> Res.string.tool_stamp
            Tool.DITHER -> Res.string.tool_dither
            Tool.REPLACE -> Res.string.tool_replace
        }

    fun toolIcon(tool: Tool): PixelIcon =
        when (tool) {
            Tool.PENCIL -> PixelIcon.PENCIL
            Tool.ERASER -> PixelIcon.ERASER
            Tool.FILL -> PixelIcon.FILL
            Tool.PICKER -> PixelIcon.PICKER
            Tool.LINE -> PixelIcon.LINE
            Tool.RECTANGLE -> PixelIcon.RECTANGLE
            Tool.ELLIPSE -> PixelIcon.ELLIPSE
            Tool.MOVE -> PixelIcon.MOVE
            Tool.STAMP -> PixelIcon.STAMP
            Tool.DITHER -> PixelIcon.DITHER
            Tool.REPLACE -> PixelIcon.REPLACE
        }

    fun palette(id: String): StringResource =
        when (id) {
            "rainbow" -> Res.string.palette_rainbow
            "pastel" -> Res.string.palette_pastel
            "forest" -> Res.string.palette_forest
            "ocean" -> Res.string.palette_ocean
            "retro" -> Res.string.palette_retro
            else -> Res.string.palette_bright
        }

    fun stamp(id: String): StringResource =
        when (id) {
            "heart" -> Res.string.stamp_heart
            "star" -> Res.string.stamp_star
            "smiley" -> Res.string.stamp_smiley
            "flower" -> Res.string.stamp_flower
            "cat" -> Res.string.stamp_cat
            "sword" -> Res.string.stamp_sword
            "mushroom" -> Res.string.stamp_mushroom
            "rocket" -> Res.string.stamp_rocket
            "ghost" -> Res.string.stamp_ghost
            else -> Res.string.stamp_tree
        }

    fun template(id: String): StringResource =
        when (id) {
            "house" -> Res.string.tpl_house
            "fish" -> Res.string.tpl_fish
            "dinosaur" -> Res.string.tpl_dinosaur
            "robot" -> Res.string.tpl_robot
            else -> Res.string.tpl_flower
        }
}

@Composable
fun str(res: StringResource): String = stringResource(res)

@Composable
fun str(res: StringResource, vararg args: Any): String = stringResource(res, *args)
