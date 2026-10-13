package app.pixelpals.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pixelpals.model.Argb
import app.pixelpals.model.PixelBuffer
import app.pixelpals.ui.editor.CanvasRenderer
import app.pixelpals.ui.theme.LocalCanvasColors
import app.pixelpals.ui.theme.PixelIcon
import app.pixelpals.ui.theme.PixelIconImage

/** Whether text labels are shown under icon buttons (a parent setting). */
val LocalShowLabels = staticCompositionLocalOf { true }

/** Minimum touch target for kids. */
val KidTarget: Dp = 56.dp

/**
 * A big icon button with a text label under it. [label] is always used as the accessibility
 * description, even when labels are hidden visually.
 */
@Composable
fun KidButton(
    icon: PixelIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    iconSize: Dp = 28.dp,
    minSize: Dp = KidTarget,
    container: Color = Color.Transparent,
    showLabel: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val bg =
        when {
            selected -> colors.primary
            else -> container
        }
    val fg =
        when {
            !enabled -> colors.onSurface.copy(alpha = 0.35f)
            selected -> colors.onPrimary
            else -> colors.onSurface
        }
    Column(
        modifier
            .defaultMinSize(minSize, minSize)
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                this.selected = selected
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PixelIconImage(icon, size = iconSize, tint = fg, accent = if (selected) colors.secondaryContainer else colors.secondary)
        if (showLabel && LocalShowLabels.current) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** A round color swatch. Transparent is shown as a checkerboard. */
@Composable
fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    description: String,
) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Box(
        modifier
            .size(maxOf(size, KidTarget))
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(color))
                .border(if (selected) 4.dp else 2.dp, ring, CircleShape),
        )
    }
}

/** Draws a checkerboard behind transparent content. */
fun DrawScope.checkerboard(cell: Float, a: Color, b: Color) {
    drawRect(a)
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            if ((x + y) % 2 == 1) {
                drawRect(
                    b,
                    Offset(x * cell, y * cell),
                    Size(cell, cell),
                )
            }
        }
    }
}

/** A crisp, nearest-neighbor thumbnail of [buffer] on a checkerboard. */
@Composable
fun PixelThumbnail(buffer: PixelBuffer, modifier: Modifier = Modifier, description: String? = null) {
    val bitmap = remember(buffer) { CanvasRenderer.bitmapOf(buffer) }
    val checker = LocalCanvasColors.current
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).drawBehind { checkerboard(8f, checker.checkerA, checker.checkerB) },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap,
            contentDescription = description,
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Playful two-button confirmation dialog with big buttons. */
@Composable
fun KidConfirmDialog(
    title: String,
    yes: String,
    no: String,
    onYes: () -> Unit,
    onDismiss: () -> Unit,
    text: String? = null,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = text?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
        confirmButton = {
            Button(
                onClick = {
                    onYes()
                    onDismiss()
                },
                colors =
                    if (destructive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                modifier = Modifier.height(KidTarget),
            ) { Text(yes, style = MaterialTheme.typography.titleMedium) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.height(KidTarget)) {
                Text(no, style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}

/** A rounded panel used for side panels and toolbars. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 1.dp) {
        content()
    }
}

/** Section title inside panels. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

/** Row of a label and trailing content, at least kid-target tall. */
@Composable
fun SettingRow(label: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().defaultMinSize(minHeight = KidTarget), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

/** Hex label for accessibility descriptions. */
fun colorName(argb: Int): String =
    if (argb ==
        Argb.TRANSPARENT
    ) {
        "transparent"
    } else {
        "#" + (argb and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()
    }
