package app.pixelpals.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pixelpals.model.Drawing
import app.pixelpals.model.EditorAction
import app.pixelpals.model.LayerInfo
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.KidConfirmDialog
import app.pixelpals.ui.common.KidTarget
import app.pixelpals.ui.common.PixelThumbnail
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.add_layer
import app.pixelpals.ui.res.cancel
import app.pixelpals.ui.res.delete
import app.pixelpals.ui.res.delete_layer_confirm
import app.pixelpals.ui.res.hide_layer
import app.pixelpals.ui.res.layer_n
import app.pixelpals.ui.res.locked
import app.pixelpals.ui.res.move_down
import app.pixelpals.ui.res.move_up
import app.pixelpals.ui.res.name_label
import app.pixelpals.ui.res.no_keep
import app.pixelpals.ui.res.ok
import app.pixelpals.ui.res.rename
import app.pixelpals.ui.res.select_layer
import app.pixelpals.ui.res.show_layer
import app.pixelpals.ui.res.yes_delete
import app.pixelpals.ui.theme.PixelIcon

/** Layer list, top layer first, with add / show-hide / reorder / rename / delete. */
@Composable
fun LayersPanel(controller: EditorController, modifier: Modifier = Modifier) {
    val state = controller.state
    val d = state.drawing
    var confirmDelete by remember { mutableStateOf<LayerInfo?>(null) }
    var renaming by remember { mutableStateOf<LayerInfo?>(null) }
    val nextName = str(Res.string.layer_n, d.layers.size + 1)
    Column(modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((index, layer) in d.layers.withIndex().reversed()) {
            LayerRow(
                controller,
                layer,
                selected = layer.id == state.layerId,
                canMoveUp = index < d.layers.lastIndex,
                canMoveDown = index > 0,
                canDelete = d.layers.size > 1 && !layer.locked,
                onDelete = { confirmDelete = layer },
                onRename = { renaming = layer },
            )
        }
        KidButton(
            PixelIcon.PLUS,
            str(Res.string.add_layer),
            onClick = { controller.dispatch(EditorAction.AddLayer(nextName)) },
            enabled = d.layers.size < Drawing.MAX_LAYERS,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    confirmDelete?.let { layer ->
        KidConfirmDialog(
            title = str(Res.string.delete_layer_confirm, layer.name),
            yes = str(Res.string.yes_delete),
            no = str(Res.string.no_keep),
            onYes = { controller.dispatch(EditorAction.DeleteLayer(layer.id)) },
            onDismiss = { confirmDelete = null },
        )
    }
    renaming?.let { layer ->
        RenameDialog(layer.name, onDismiss = { renaming = null }) { controller.dispatch(EditorAction.RenameLayer(layer.id, it)) }
    }
}

@Composable
private fun LayerRow(
    controller: EditorController,
    layer: LayerInfo,
    selected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onRename: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val state = controller.state
    val selectDesc = str(Res.string.select_layer, layer.name)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) colors.primary else colors.outline.copy(alpha = 0.3f),
                MaterialTheme.shapes.small,
            )
            .padding(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = KidTarget)
                .clickable(role = Role.Button) { controller.dispatch(EditorAction.SelectLayer(layer.id)) }
                .semantics(mergeDescendants = true) {
                    contentDescription = selectDesc
                    this.selected = selected
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelThumbnail(state.drawing.cel(state.frameIndex, layer.id), Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(layer.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (layer.locked) Text(str(Res.string.locked), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            KidButton(
                if (layer.visible) PixelIcon.EYE else PixelIcon.EYE_OFF,
                if (layer.visible) str(Res.string.hide_layer, layer.name) else str(Res.string.show_layer, layer.name),
                onClick = { controller.dispatch(EditorAction.ToggleLayerVisible(layer.id)) },
                iconSize = 22.dp,
                showLabel = false,
            )
        }
        if (selected) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                KidButton(PixelIcon.UP, str(Res.string.move_up), {
                    controller.dispatch(EditorAction.MoveLayer(layer.id, 1))
                }, enabled = canMoveUp, iconSize = 20.dp)
                KidButton(PixelIcon.DOWN, str(Res.string.move_down), {
                    controller.dispatch(EditorAction.MoveLayer(layer.id, -1))
                }, enabled = canMoveDown, iconSize = 20.dp)
                KidButton(PixelIcon.PENCIL, str(Res.string.rename), onRename, enabled = !layer.locked, iconSize = 20.dp)
                KidButton(PixelIcon.TRASH, str(Res.string.delete), onDelete, enabled = canDelete, iconSize = 20.dp)
            }
        }
    }
}

/** Small text dialog for renaming (optional for kids; never required to draw). */
@Composable
fun RenameDialog(initial: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.rename)) },
        text = { OutlinedTextField(text, { text = it.take(40) }, label = { Text(str(Res.string.name_label)) }, singleLine = true) },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onRename(text.trim())
                    onDismiss()
                },
                modifier = Modifier.defaultMinSize(minHeight = KidTarget),
            ) { Text(str(Res.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = KidTarget)) { Text(str(Res.string.cancel)) }
        },
    )
}
