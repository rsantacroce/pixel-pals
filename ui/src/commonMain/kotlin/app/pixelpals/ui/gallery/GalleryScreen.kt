package app.pixelpals.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pixelpals.io.ProjectSummary
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.KidConfirmDialog
import app.pixelpals.ui.common.PixelThumbnail
import app.pixelpals.ui.common.str
import app.pixelpals.ui.editor.RenameDialog
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.app_name
import app.pixelpals.ui.res.delete
import app.pixelpals.ui.res.delete_drawing_confirm
import app.pixelpals.ui.res.duplicate
import app.pixelpals.ui.res.for_grownups
import app.pixelpals.ui.res.gallery_empty
import app.pixelpals.ui.res.gallery_title
import app.pixelpals.ui.res.more_actions
import app.pixelpals.ui.res.new_drawing
import app.pixelpals.ui.res.no_keep
import app.pixelpals.ui.res.open_drawing
import app.pixelpals.ui.res.recycle_bin
import app.pixelpals.ui.res.rename
import app.pixelpals.ui.res.yes_delete
import app.pixelpals.ui.theme.PixelIcon
import app.pixelpals.ui.theme.PixelIconImage

/** Home screen: the child's drawings as a grid plus a big "+ New Drawing" card. */
@Composable
fun GalleryScreen(
    drawings: List<ProjectSummary>,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    onDuplicate: (ProjectSummary) -> Unit,
    onRename: (ProjectSummary, String) -> Unit,
    onDelete: (ProjectSummary) -> Unit,
    onRecycleBin: () -> Unit,
    onGrownUps: () -> Unit,
) {
    var deleting by remember { mutableStateOf<ProjectSummary?>(null) }
    var renaming by remember { mutableStateOf<ProjectSummary?>(null) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("gallery")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelIconImage(PixelIcon.STAR, size = 36.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(str(Res.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(str(Res.string.gallery_title), style = MaterialTheme.typography.titleMedium)
            }
            KidButton(PixelIcon.TRASH, str(Res.string.recycle_bin), onRecycleBin)
            KidButton(PixelIcon.GEAR, str(Res.string.for_grownups), onGrownUps)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { NewDrawingCard(onNew) }
            items(drawings, key = { it.id }) { d ->
                DrawingCard(d, onOpen = { onOpen(d.id) }, onDuplicate = { onDuplicate(d) }, onRename = { renaming = d }, onDelete = {
                    deleting =
                        d
                })
            }
        }
        if (drawings.isEmpty()) {
            Text(
                str(Res.string.gallery_empty),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }
    }
    deleting?.let { d ->
        KidConfirmDialog(
            title = str(Res.string.delete_drawing_confirm, d.name),
            yes = str(Res.string.yes_delete),
            no = str(Res.string.no_keep),
            onYes = { onDelete(d) },
            onDismiss = { deleting = null },
        )
    }
    renaming?.let { d -> RenameDialog(d.name, onDismiss = { renaming = null }) { onRename(d, it) } }
}

@Composable
private fun NewDrawingCard(onNew: () -> Unit) {
    val label = str(Res.string.new_drawing)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .aspectRatio(0.85f)
                .clip(MaterialTheme.shapes.large)
                .clickable(role = Role.Button, onClick = onNew)
                .semantics(mergeDescendants = true) { contentDescription = label }
                .testTag("new-drawing"),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            PixelIconImage(PixelIcon.PLUS, size = 72.dp, tint = MaterialTheme.colorScheme.onPrimary)
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DrawingCard(d: ProjectSummary, onOpen: () -> Unit, onDuplicate: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val openLabel = str(Res.string.open_drawing, d.name)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.aspectRatio(0.85f).testTag("drawing-${d.id}"),
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .semantics { contentDescription = openLabel },
                contentAlignment = Alignment.Center,
            ) {
                PixelThumbnail(d.thumbnail, Modifier.fillMaxSize().padding(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    KidButton(PixelIcon.MORE, str(Res.string.more_actions, d.name), { menu = true }, iconSize = 24.dp, showLabel = false)
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        MenuItem(PixelIcon.COPY, str(Res.string.duplicate)) {
                            menu = false
                            onDuplicate()
                        }
                        MenuItem(PixelIcon.PENCIL, str(Res.string.rename)) {
                            menu = false
                            onRename()
                        }
                        MenuItem(PixelIcon.TRASH, str(Res.string.delete)) {
                            menu = false
                            onDelete()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: PixelIcon, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.titleMedium) },
        leadingIcon = { PixelIconImage(icon, size = 24.dp) },
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
