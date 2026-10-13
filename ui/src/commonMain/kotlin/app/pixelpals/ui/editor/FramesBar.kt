package app.pixelpals.ui.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.pixelpals.io.ProjectCodec
import app.pixelpals.model.Drawing
import app.pixelpals.model.EditorAction
import app.pixelpals.model.PixelBuffer
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.PixelThumbnail
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.add_frame
import app.pixelpals.ui.res.delete_frame
import app.pixelpals.ui.res.duplicate_frame
import app.pixelpals.ui.res.faster
import app.pixelpals.ui.res.frame_n
import app.pixelpals.ui.res.move_frame_left
import app.pixelpals.ui.res.move_frame_right
import app.pixelpals.ui.res.onion_skin
import app.pixelpals.ui.res.pause
import app.pixelpals.ui.res.play
import app.pixelpals.ui.res.slower
import app.pixelpals.ui.res.speed
import app.pixelpals.ui.theme.PixelIcon
import kotlinx.coroutines.delay

/** Animation strip: frame thumbnails plus add/copy/delete/reorder, play, speed and onion skin. */
@Composable
fun FramesBar(controller: EditorController, playing: Boolean, onPlayingChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val state = controller.state
    val d = state.drawing
    LaunchedEffect(playing, d.fps, d.frames.size) {
        if (!playing || d.frames.size < 2) return@LaunchedEffect
        while (true) {
            delay(1000L / d.fps)
            controller.dispatch(EditorAction.SelectFrame((controller.state.frameIndex + 1) % controller.state.drawing.frames.size))
        }
    }
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KidButton(
            if (playing) PixelIcon.PAUSE else PixelIcon.PLAY,
            str(if (playing) Res.string.pause else Res.string.play),
            onClick = { onPlayingChange(!playing) },
            enabled = d.frames.size > 1,
            selected = playing,
        )
        for (i in d.frames.indices) {
            val selected = i == state.frameIndex
            val desc = str(Res.string.frame_n, i + 1)
            Box(
                Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        if (selected) 3.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        MaterialTheme.shapes.small,
                    )
                    .clickable(enabled = !playing, role = Role.Button) { controller.dispatch(EditorAction.SelectFrame(i)) }
                    .semantics {
                        contentDescription = desc
                        this.selected = selected
                    }
                    .padding(4.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                val frame = d.frames[i]
                val thumb = remember(frame, d.layers) { frameThumb(d, i) }
                PixelThumbnail(thumb, Modifier.size(52.dp))
                Text("${i + 1}", style = MaterialTheme.typography.labelSmall)
            }
        }
        val editable = !playing
        KidButton(
            PixelIcon.PLUS,
            str(Res.string.add_frame),
            { controller.dispatch(EditorAction.AddFrame) },
            enabled =
                editable && d.frames.size < Drawing.MAX_FRAMES,
        )
        KidButton(
            PixelIcon.COPY,
            str(Res.string.duplicate_frame),
            { controller.dispatch(EditorAction.DuplicateFrame) },
            enabled =
                editable && d.frames.size < Drawing.MAX_FRAMES,
        )
        KidButton(
            PixelIcon.UNDO,
            str(Res.string.move_frame_left),
            { controller.dispatch(EditorAction.MoveFrame(-1)) },
            enabled =
                editable && state.frameIndex > 0,
        )
        KidButton(
            PixelIcon.REDO,
            str(Res.string.move_frame_right),
            { controller.dispatch(EditorAction.MoveFrame(1)) },
            enabled =
                editable && state.frameIndex < d.frames.lastIndex,
        )
        KidButton(
            PixelIcon.TRASH,
            str(Res.string.delete_frame),
            { controller.dispatch(EditorAction.DeleteFrame) },
            enabled =
                editable && d.frames.size > 1,
        )
        KidButton(PixelIcon.ONION, str(Res.string.onion_skin), {
            controller.dispatch(EditorAction.ToggleOnionSkin)
        }, selected = state.onionSkin)
        KidButton(PixelIcon.DOWN, str(Res.string.slower), { controller.dispatch(EditorAction.SetFps(d.fps - 1)) }, enabled = d.fps > 1)
        Text(str(Res.string.speed, d.fps), style = MaterialTheme.typography.titleSmall)
        KidButton(PixelIcon.UP, str(Res.string.faster), { controller.dispatch(EditorAction.SetFps(d.fps + 1)) }, enabled = d.fps < 24)
    }
}

private fun frameThumb(d: Drawing, index: Int): PixelBuffer =
    ProjectCodec.thumbnail(d.copy(frames = listOf(d.frames[index])))
