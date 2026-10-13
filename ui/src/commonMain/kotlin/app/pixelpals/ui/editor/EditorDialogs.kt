package app.pixelpals.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.pixelpals.io.PngExport
import app.pixelpals.io.ProjectRepository
import app.pixelpals.model.EditorAction
import app.pixelpals.model.Stamps
import app.pixelpals.ui.AppController
import app.pixelpals.ui.common.KidTarget
import app.pixelpals.ui.common.Names
import app.pixelpals.ui.common.PixelThumbnail
import app.pixelpals.ui.common.SectionTitle
import app.pixelpals.ui.common.str
import app.pixelpals.ui.platform.ExportTarget
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.background
import app.pixelpals.ui.res.close
import app.pixelpals.ui.res.export_done
import app.pixelpals.ui.res.export_failed
import app.pixelpals.ui.res.export_kind
import app.pixelpals.ui.res.export_pixels
import app.pixelpals.ui.res.export_scale
import app.pixelpals.ui.res.export_size
import app.pixelpals.ui.res.export_title
import app.pixelpals.ui.res.exporting
import app.pixelpals.ui.res.kind_animation
import app.pixelpals.ui.res.kind_picture
import app.pixelpals.ui.res.kind_sheet
import app.pixelpals.ui.res.pick_stamp
import app.pixelpals.ui.res.save_as
import app.pixelpals.ui.res.save_to_pictures
import app.pixelpals.ui.res.see_through
import app.pixelpals.ui.res.share
import app.pixelpals.ui.res.shortcut_mirror
import app.pixelpals.ui.res.shortcut_tools
import app.pixelpals.ui.res.shortcut_undo
import app.pixelpals.ui.res.shortcut_view
import app.pixelpals.ui.res.shortcuts_title
import app.pixelpals.ui.res.white
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ExportKind { PICTURE, ANIMATION, SHEET }

/** Export: scale (1×–16×), background, picture / animated GIF / sprite sheet, then share or save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(app: AppController, controller: EditorController, onDismiss: () -> Unit) {
    val settings = app.settings
    val drawing = controller.state.drawing
    var kind by remember { mutableStateOf(ExportKind.PICTURE) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val doneText = str(Res.string.export_done)
    val failText = str(Res.string.export_failed)

    fun run(target: ExportTarget) {
        busy = true
        status = null
        val snapshot = controller.state.drawing
        val frame = controller.state.frameIndex
        val scale = settings.exportScale
        val white = settings.exportWhiteBackground
        val base = ProjectRepository.safeFileName(controller.project.name)
        scope.launch {
            val (bytes, name, mime) =
                withContext(Dispatchers.Default) {
                    when (kind) {
                        ExportKind.PICTURE -> Triple(PngExport.render(snapshot, frame, scale, white), "$base.png", "image/png")
                        ExportKind.ANIMATION -> Triple(PngExport.gif(snapshot, scale, white), "$base.gif", "image/gif")
                        ExportKind.SHEET -> Triple(PngExport.spriteSheet(snapshot, scale, white), "$base-sheet.png", "image/png")
                    }
                }
            app.platform.export(target, bytes, name, mime) { ok ->
                busy = false
                status = if (ok) doneText else failText
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (drawing.frames.size > 1) {
                    SectionTitle(str(Res.string.export_kind))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(str(Res.string.kind_picture), kind == ExportKind.PICTURE) { kind = ExportKind.PICTURE }
                        Chip(str(Res.string.kind_animation), kind == ExportKind.ANIMATION) { kind = ExportKind.ANIMATION }
                        Chip(str(Res.string.kind_sheet), kind == ExportKind.SHEET) { kind = ExportKind.SHEET }
                    }
                }
                SectionTitle(str(Res.string.export_size))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (s in PngExport.SCALES) {
                        Chip(str(Res.string.export_scale, s), settings.exportScale == s) { app.updateSettings { it.copy(exportScale = s) } }
                    }
                }
                val (w, h) = PngExport.outputSize(drawing, settings.exportScale)
                Text(str(Res.string.export_pixels, w, h), style = MaterialTheme.typography.bodyMedium)
                SectionTitle(str(Res.string.background))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(str(Res.string.see_through), !settings.exportWhiteBackground) {
                        app.updateSettings { it.copy(exportWhiteBackground = false) }
                    }
                    Chip(str(Res.string.white), settings.exportWhiteBackground) {
                        app.updateSettings { it.copy(exportWhiteBackground = true) }
                    }
                }
                for (target in app.platform.exportTargets) {
                    Button(onClick = { run(target) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(KidTarget)) {
                        Text(
                            str(
                                when (target) {
                                    ExportTarget.SHARE -> Res.string.share
                                    ExportTarget.SAVE_TO_DEVICE -> Res.string.save_to_pictures
                                    ExportTarget.SAVE_AS -> Res.string.save_as
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                if (busy) Text(str(Res.string.exporting))
                status?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = KidTarget)) { Text(str(Res.string.close)) }
        },
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = {
        Text(label, style = MaterialTheme.typography.titleSmall)
    }, modifier = Modifier.height(48.dp))
}

/** Grid of stamps; choosing one selects the Stamp tool. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StampPickerDialog(controller: EditorController, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.pick_stamp)) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (stamp in Stamps.all) {
                    val name = str(Names.stamp(stamp.id))
                    Column(
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.Button) {
                                controller.dispatch(EditorAction.SelectStamp(stamp.id))
                                onDismiss()
                            }
                            .semantics(mergeDescendants = true) { contentDescription = name }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PixelThumbnail(stamp.pixels, Modifier.size(64.dp))
                        Text(name, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = KidTarget)) { Text(str(Res.string.close)) }
        },
    )
}

/** Keyboard shortcut cheat sheet (desktop, opened with "?"). */
@Composable
fun ShortcutsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.shortcuts_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (r in listOf(
                    Res.string.shortcut_tools,
                    Res.string.shortcut_mirror,
                    Res.string.shortcut_undo,
                    Res.string.shortcut_view,
                )) {
                    Text(str(r), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = KidTarget)) { Text(str(Res.string.close)) }
        },
    )
}
