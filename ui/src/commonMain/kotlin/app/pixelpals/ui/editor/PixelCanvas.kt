package app.pixelpals.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.pixelpals.model.EditorAction
import app.pixelpals.model.Tool
import app.pixelpals.ui.theme.LocalCanvasColors
import kotlin.math.floor
import kotlin.math.roundToInt

/** Zoom and pan of the canvas inside its view. Scale is screen pixels per canvas pixel. */
@Stable
class Viewport(private val canvasWidth: Int, private val canvasHeight: Int) {
    var scale: Float by mutableFloatStateOf(1f)
        private set
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set
    var viewSize: IntSize = IntSize.Zero
        private set
    private var userAdjusted = false

    private val fitScale: Float
        get() =
            if (viewSize ==
                IntSize.Zero
            ) {
                1f
            } else {
                minOf(viewSize.width.toFloat() / canvasWidth, viewSize.height.toFloat() / canvasHeight) * 0.94f
            }

    private val minScale: Float get() = fitScale * 0.5f
    private val maxScale: Float get() = maxOf(fitScale * 8f, 96f)

    fun onViewSize(size: IntSize) {
        viewSize = size
        if (!userAdjusted) fit(userAction = false)
    }

    /** Centers the canvas at the largest size that fits. */
    fun fit(userAction: Boolean = true) {
        if (userAction) userAdjusted = false
        scale = fitScale
        offset = Offset((viewSize.width - canvasWidth * scale) / 2f, (viewSize.height - canvasHeight * scale) / 2f)
    }

    /** Multiplies the zoom by [factor] keeping [focus] (view coordinates) fixed. */
    fun zoom(factor: Float, focus: Offset = Offset(viewSize.width / 2f, viewSize.height / 2f)) {
        val newScale = (scale * factor).coerceIn(minScale, maxScale)
        val f = newScale / scale
        offset = focus - (focus - offset) * f
        scale = newScale
        userAdjusted = true
        clamp()
    }

    fun pan(delta: Offset) {
        offset += delta
        userAdjusted = true
        clamp()
    }

    /** Keeps at least a quarter of the canvas on screen so it can't get lost. */
    private fun clamp() {
        val w = canvasWidth * scale
        val h = canvasHeight * scale
        val marginX = minOf(w, viewSize.width.toFloat()) / 4
        val marginY = minOf(h, viewSize.height.toFloat()) / 4
        offset =
            Offset(
                offset.x.coerceIn(marginX - w, viewSize.width - marginX),
                offset.y.coerceIn(marginY - h, viewSize.height - marginY),
            )
    }

    /** Canvas pixel under a view position (may be out of bounds). */
    fun pixelAt(pos: Offset): Pair<Int, Int> =
        floor((pos.x - offset.x) / scale).toInt() to floor((pos.y - offset.y) / scale).toInt()
}

/**
 * The drawing surface. One finger / left mouse draws; two fingers pinch-zoom and pan (cancelling any
 * stroke that started); middle mouse or Space+drag pans; Ctrl/Cmd+scroll zooms; plain scroll pans.
 */
@Composable
fun PixelCanvas(
    controller: EditorController,
    viewport: Viewport,
    showGrid: Boolean,
    inputEnabled: Boolean,
    spaceHeld: () -> Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val state = controller.state
    val colors = LocalCanvasColors.current
    val enabled by rememberUpdatedState(inputEnabled)
    var hover by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val checkerFine =
        remember(state.drawing.width, state.drawing.height, colors) {
            checkerBitmap(state.drawing.width * 2, state.drawing.height * 2, colors.checkerA, colors.checkerB)
        }
    val checkerCoarse =
        remember(state.drawing.width, state.drawing.height, colors) {
            checkerBitmap(state.drawing.width, state.drawing.height, colors.checkerA, colors.checkerB)
        }

    Canvas(
        modifier
            .onSizeChanged { viewport.onViewSize(it) }
            .testTag("canvas")
            .semantics { contentDescription = description }
            .pointerInput(controller, viewport) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val first = currentEvent
                    val isMouse = down.type == PointerType.Mouse
                    val panMode = first.buttons.isTertiaryPressed || spaceHeld()
                    if (isMouse && !panMode && !first.buttons.isPrimaryPressed) return@awaitEachGesture
                    var drawing = !panMode && enabled
                    var transforming = panMode
                    if (drawing) {
                        val (x, y) = viewport.pixelAt(down.position)
                        controller.dispatch(EditorAction.Press(x, y))
                    }
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed == 0) break
                        if (pressed >= 2 && !transforming) {
                            if (drawing) controller.dispatch(EditorAction.CancelStroke)
                            drawing = false
                            transforming = true
                        }
                        if (transforming) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) viewport.zoom(zoom, event.calculateCentroid(useCurrent = true))
                            viewport.pan(event.calculatePan())
                        } else if (drawing) {
                            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                            for (h in change.historical) {
                                val (x, y) = viewport.pixelAt(h.position)
                                controller.dispatch(EditorAction.Drag(x, y))
                            }
                            val (x, y) = viewport.pixelAt(change.position)
                            controller.dispatch(EditorAction.Drag(x, y))
                            if (isMouse) hover = x to y
                        }
                        event.changes.forEach { it.consume() }
                    }
                    if (drawing) controller.dispatch(EditorAction.Release)
                }
            }
            .pointerInput(viewport) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        when (event.type) {
                            PointerEventType.Scroll -> {
                                val delta = change.scrollDelta
                                val mods = event.keyboardModifiers
                                if (mods.isCtrlPressed || mods.isMetaPressed) {
                                    viewport.zoom(if (delta.y < 0) 1.15f else 1 / 1.15f, change.position)
                                } else {
                                    viewport.pan(Offset(-delta.x, -delta.y) * 40f)
                                }
                                change.consume()
                            }
                            PointerEventType.Move -> if (change.type == PointerType.Mouse) hover = viewport.pixelAt(change.position)
                            PointerEventType.Exit -> hover = null
                            else -> Unit
                        }
                    }
                }
            },
    ) {
        drawRect(colors.backdrop)
        val w = state.drawing.width
        val h = state.drawing.height
        val dstOffset = IntOffset(viewport.offset.x.roundToInt(), viewport.offset.y.roundToInt())
        val dstSize = IntSize((w * viewport.scale).roundToInt().coerceAtLeast(1), (h * viewport.scale).roundToInt().coerceAtLeast(1))
        val cell = dstSize.width.toFloat() / w
        val checker = if (cell >= 12f) checkerFine else checkerCoarse
        drawImage(checker, IntOffset.Zero, IntSize(checker.width, checker.height), dstOffset, dstSize, filterQuality = FilterQuality.None)
        controller.onion?.let {
            drawImage(it, IntOffset.Zero, IntSize(w, h), dstOffset, dstSize, alpha = 0.3f, filterQuality = FilterQuality.None)
        }
        // Reading version subscribes this draw pass to bitmap updates.
        controller.renderer.version
        drawImage(controller.renderer.image, IntOffset.Zero, IntSize(w, h), dstOffset, dstSize, filterQuality = FilterQuality.None)
        val origin = Offset(dstOffset.x.toFloat(), dstOffset.y.toFloat())
        val area = Size(dstSize.width.toFloat(), dstSize.height.toFloat())
        clipRect(origin.x, origin.y, origin.x + area.width, origin.y + area.height) {
            if (showGrid && cell >= 6f) drawGrid(origin, cell, w, h, colors.grid)
            drawMirrorAxes(controller, origin, area, colors.axis)
            hover?.let { (hx, hy) -> drawHover(controller, origin, cell, hx, hy) }
        }
        drawRect(colors.grid, origin, area, style = Stroke(2f))
    }
}

private fun DrawScope.drawGrid(origin: Offset, cell: Float, w: Int, h: Int, color: Color) {
    for (x in 1 until w) {
        val px = origin.x + x * cell
        drawLine(color, Offset(px, origin.y), Offset(px, origin.y + h * cell), strokeWidth = 1f)
    }
    for (y in 1 until h) {
        val py = origin.y + y * cell
        drawLine(color, Offset(origin.x, py), Offset(origin.x + w * cell, py), strokeWidth = 1f)
    }
}

private fun DrawScope.drawMirrorAxes(controller: EditorController, origin: Offset, area: Size, color: Color) {
    val mirror = controller.state.settings.mirror
    val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
    if (mirror.horizontal) {
        val x = origin.x + area.width / 2
        drawLine(color, Offset(x, origin.y), Offset(x, origin.y + area.height), strokeWidth = 4f, pathEffect = dash)
    }
    if (mirror.vertical) {
        val y = origin.y + area.height / 2
        drawLine(color, Offset(origin.x, y), Offset(origin.x + area.width, y), strokeWidth = 4f, pathEffect = dash)
    }
}

private fun DrawScope.drawHover(controller: EditorController, origin: Offset, cell: Float, x: Int, y: Int) {
    val s = controller.state.settings
    val size = if (s.tool.usesBrush) s.brushSize else 1
    val start = if (size == 3) -1 else 0
    val color = if (s.tool == Tool.ERASER) Color.Gray else Color(controller.state.color)
    drawRect(
        color,
        Offset(origin.x + (x + start) * cell, origin.y + (y + start) * cell),
        Size(size * cell, size * cell),
        style = Stroke(maxOf(2f, cell / 8)),
    )
}

private fun checkerBitmap(w: Int, h: Int, a: Color, b: Color): ImageBitmap {
    val ai = a.toArgb()
    val bi = b.toArgb()
    return CanvasRenderer.bitmapOf(w, h, IntArray(w * h) { if ((it % w + it / w) % 2 == 0) ai else bi })
}
