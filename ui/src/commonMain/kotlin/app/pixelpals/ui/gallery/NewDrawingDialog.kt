package app.pixelpals.ui.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import app.pixelpals.model.Challenges
import app.pixelpals.model.ColoringPages
import app.pixelpals.model.Drawing
import app.pixelpals.model.PixelBuffer
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.KidTarget
import app.pixelpals.ui.common.Names
import app.pixelpals.ui.common.PixelThumbnail
import app.pixelpals.ui.common.SectionTitle
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.cancel
import app.pixelpals.ui.res.challenges
import app.pixelpals.ui.res.coloring_pages
import app.pixelpals.ui.res.create
import app.pixelpals.ui.res.custom_size
import app.pixelpals.ui.res.from_photo
import app.pixelpals.ui.res.height
import app.pixelpals.ui.res.pick_size
import app.pixelpals.ui.res.size_big
import app.pixelpals.ui.res.size_dims
import app.pixelpals.ui.res.size_medium
import app.pixelpals.ui.res.size_more
import app.pixelpals.ui.res.size_small
import app.pixelpals.ui.res.size_tiny
import app.pixelpals.ui.res.width
import app.pixelpals.ui.theme.PixelIcon
import app.pixelpals.ui.theme.PixelIconImage
import org.jetbrains.compose.resources.StringResource

/** The four friendly canvas sizes. */
val CANVAS_SIZES: List<Pair<StringResource, Int>> =
    listOf(
        Res.string.size_tiny to 16,
        Res.string.size_small to 32,
        Res.string.size_medium to 64,
        Res.string.size_big to 128,
    )

/** What the child chose to start from. */
sealed interface NewDrawingChoice {
    data class Blank(val width: Int, val height: Int) : NewDrawingChoice

    data class Template(val id: String) : NewDrawingChoice

    data class Challenge(val id: String) : NewDrawingChoice

    data class Photo(val size: Int) : NewDrawingChoice
}

/** Pick a size, a coloring page, a challenge or a photo. No typing needed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewDrawingDialog(allowCustomSizes: Boolean, onChoice: (NewDrawingChoice) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf(false) }
    if (custom) {
        CustomSizeDialog(onCreate = { w, h -> onChoice(NewDrawingChoice.Blank(w, h)) }, onDismiss = { custom = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.pick_size), style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((label, n) in CANVAS_SIZES) {
                        SizeCard(str(label), n, Modifier.testTag("size-$n")) { onChoice(NewDrawingChoice.Blank(n, n)) }
                    }
                    if (allowCustomSizes) {
                        KidButton(PixelIcon.PLUS, str(Res.string.size_more), { custom = true }, minSize = 96.dp)
                    }
                }
                SectionTitle(str(Res.string.coloring_pages))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (id in ColoringPages.ids) {
                        PictureChoice(ColoringPages.outline(id), str(Names.template(id))) { onChoice(NewDrawingChoice.Template(id)) }
                    }
                }
                SectionTitle(str(Res.string.challenges))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in Challenges.all) {
                        PictureChoice(c.target, str(Names.stamp(c.id))) { onChoice(NewDrawingChoice.Challenge(c.id)) }
                    }
                }
                KidButton(PixelIcon.PHOTO, str(Res.string.from_photo), { photo = !photo }, selected = photo)
                if (photo) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for ((label, n) in CANVAS_SIZES) SizeCard(str(label), n) { onChoice(NewDrawingChoice.Photo(n)) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = KidTarget)) { Text(str(Res.string.cancel)) }
        },
    )
}

@Composable
private fun SizeCard(label: String, n: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dims = str(Res.string.size_dims, n, n)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = "$label, $dims" },
    ) {
        Column(Modifier.width(96.dp).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Bigger canvases get a bigger grid icon so the choice is visual, not just words.
            PixelIconImage(PixelIcon.GRID, size = (16 + n / 4).coerceAtMost(48).dp)
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(dims, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PictureChoice(picture: PixelBuffer, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelThumbnail(picture, Modifier.size(72.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CustomSizeDialog(onCreate: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var w by remember { mutableIntStateOf(48) }
    var h by remember { mutableIntStateOf(48) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.custom_size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(str(Res.string.width), w) { w = it }
                Stepper(str(Res.string.height), h) { h = it }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(w, h) }, modifier = Modifier.defaultMinSize(minHeight = KidTarget)) { Text(str(Res.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = KidTarget)) { Text(str(Res.string.cancel)) }
        },
    )
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(80.dp))
        KidButton(PixelIcon.DOWN, "$label −8", { onChange((value - 8).coerceAtLeast(8)) }, iconSize = 20.dp)
        KidButton(PixelIcon.DOWN, "$label −1", { onChange((value - 1).coerceAtLeast(1)) }, iconSize = 14.dp)
        Text("$value", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(56.dp))
        KidButton(PixelIcon.UP, "$label +1", { onChange((value + 1).coerceAtMost(Drawing.MAX_SIZE)) }, iconSize = 14.dp)
        KidButton(PixelIcon.UP, "$label +8", { onChange((value + 8).coerceAtMost(Drawing.MAX_SIZE)) }, iconSize = 20.dp)
    }
}
