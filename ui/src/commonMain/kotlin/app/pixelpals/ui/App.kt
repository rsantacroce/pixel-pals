package app.pixelpals.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.pixelpals.io.AppSettings
import app.pixelpals.io.GalleryListing
import app.pixelpals.io.Project
import app.pixelpals.io.ProjectSummary
import app.pixelpals.io.TrashedProject
import app.pixelpals.io.currentTimeMillis
import app.pixelpals.io.decodeImage
import app.pixelpals.model.Challenges
import app.pixelpals.model.ColoringPages
import app.pixelpals.model.Drawing
import app.pixelpals.model.Palettes
import app.pixelpals.model.PixelBuffer
import app.pixelpals.model.Pixelate
import app.pixelpals.ui.common.LocalShowLabels
import app.pixelpals.ui.common.str
import app.pixelpals.ui.editor.EditorController
import app.pixelpals.ui.editor.EditorRoute
import app.pixelpals.ui.gallery.GalleryScreen
import app.pixelpals.ui.gallery.NewDrawingChoice
import app.pixelpals.ui.gallery.NewDrawingDialog
import app.pixelpals.ui.gallery.RecycleBinScreen
import app.pixelpals.ui.parent.ParentGateDialog
import app.pixelpals.ui.parent.ParentSettingsScreen
import app.pixelpals.ui.platform.ExportTarget
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.copy_of
import app.pixelpals.ui.res.corrupt_message
import app.pixelpals.ui.res.corrupt_title
import app.pixelpals.ui.res.drawing_default_name
import app.pixelpals.ui.res.export_done
import app.pixelpals.ui.res.export_failed
import app.pixelpals.ui.res.layer_background
import app.pixelpals.ui.res.layer_colors
import app.pixelpals.ui.res.layer_outline
import app.pixelpals.ui.res.ok
import app.pixelpals.ui.res.photo_error
import app.pixelpals.ui.res.save_error
import app.pixelpals.ui.theme.PixelPalsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What a parent gate is protecting. */
private enum class GateFor { SETTINGS, EMPTY_BIN }

/** Root of the shared UI. Platform entry points create one [AppController] and call this. */
@Composable
fun PixelPalsApp(app: AppController, onEditorChanged: (EditorController?) -> Unit = {}) {
    PixelPalsTheme(app.settings.theme) {
        CompositionLocalProvider(LocalShowLabels provides app.settings.showLabels) {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { AppContent(app, onEditorChanged) }
            }
        }
    }
}

@Composable
private fun AppContent(app: AppController, onEditorChanged: (EditorController?) -> Unit) {
    var routeKey by rememberSaveable { mutableStateOf(Route.Gallery.encode()) }
    val route = Route.decode(routeKey).let { if (routeKey == Route.ParentSettings.encode()) Route.ParentSettings else it }
    var listing by remember { mutableStateOf(GalleryListing(emptyList(), emptyList())) }
    var trash by remember { mutableStateOf(emptyList<TrashedProject>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var gate by remember { mutableStateOf<GateFor?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var parentStatus by remember { mutableStateOf<String?>(null) }
    val scope = app.scope

    val corruptText = str(Res.string.corrupt_message)
    val photoError = str(Res.string.photo_error)
    val doneText = str(Res.string.export_done)
    val failText = str(Res.string.export_failed)
    val saveErrorText = str(Res.string.save_error)
    val nextName = str(Res.string.drawing_default_name, listing.projects.size + 1)
    val backgroundName = str(Res.string.layer_background)
    val colorsName = str(Res.string.layer_colors)
    val outlineName = str(Res.string.layer_outline)
    val copyTemplate = str(Res.string.copy_of, "%s")

    fun go(r: Route) {
        routeKey = r.encode()
    }

    LaunchedEffect(routeKey, refresh) {
        if (route == Route.Gallery) {
            listing = app.repository.list()
            if (listing.movedAside.isNotEmpty()) message = corruptText
        }
        if (route == Route.RecycleBin) trash = app.repository.listTrash()
    }
    LaunchedEffect(app.saveError) {
        if (app.saveError) {
            message = saveErrorText
            app.saveError = false
        }
    }

    fun create(drawing: Drawing, templateId: String? = null, challengeId: String? = null) {
        val now = currentTimeMillis()
        val project = Project(Project.newId(), nextName, now, now, drawing, templateId = templateId, challengeId = challengeId)
        scope.launch {
            app.repository.save(project)
            go(Route.Editor(project.id))
        }
    }

    fun onChoice(choice: NewDrawingChoice) {
        showNew = false
        when (choice) {
            is NewDrawingChoice.Blank -> create(Drawing.blank(choice.width, choice.height, backgroundName))
            is NewDrawingChoice.Template -> create(ColoringPages.drawing(choice.id, colorsName, outlineName), templateId = choice.id)
            is NewDrawingChoice.Challenge -> {
                val c = Challenges.byId(choice.id) ?: return
                create(Drawing.blank(c.target.width, c.target.height, backgroundName), challengeId = c.id)
            }
            is NewDrawingChoice.Photo ->
                app.platform.pickImage { bytes ->
                    if (bytes == null) return@pickImage
                    scope.launch {
                        val img = withContext(Dispatchers.Default) { decodeImage(bytes) }
                        if (img == null) {
                            message = photoError
                            return@launch
                        }
                        val px = Pixelate.toCanvas(img.argb, img.width, img.height, choice.size, choice.size, Palettes.Bright.colors)
                        val d = Drawing.blank(choice.size, choice.size, backgroundName)
                        create(d.withCel(0, d.layers.first().id, PixelBuffer.of(choice.size, choice.size, px)))
                    }
                }
        }
    }

    when (route) {
        Route.Gallery ->
            GalleryScreen(
                drawings = listing.projects,
                onNew = { showNew = true },
                onOpen = { go(Route.Editor(it)) },
                onDuplicate = { d: ProjectSummary ->
                    scope.launch {
                        runCatching { app.repository.duplicate(d.id, copyTemplate.replace("%s", d.name)) }
                        refresh++
                    }
                },
                onRename = { d, name ->
                    scope.launch {
                        runCatching { app.repository.rename(d.id, name) }
                        refresh++
                    }
                },
                onDelete = { d ->
                    scope.launch {
                        app.repository.moveToTrash(d.id)
                        refresh++
                    }
                },
                onRecycleBin = { go(Route.RecycleBin) },
                onGrownUps = { gate = GateFor.SETTINGS },
            )
        is Route.Editor ->
            EditorRoute(
                app,
                route.projectId,
                onOpened = onEditorChanged,
                onFailed = { corrupt ->
                    if (corrupt) message = corruptText
                    go(Route.Gallery)
                },
                onHome = {
                    scope.launch {
                        app.flush()
                        go(Route.Gallery)
                    }
                },
            )
        Route.RecycleBin ->
            RecycleBinScreen(
                trash,
                onRestore = { id ->
                    scope.launch {
                        app.repository.restore(id)
                        refresh++
                    }
                },
                onEmpty = { gate = GateFor.EMPTY_BIN },
                onBack = { go(Route.Gallery) },
            )
        Route.ParentSettings ->
            ParentSettingsScreen(
                app,
                parentStatus,
                onExportZip = {
                    scope.launch {
                        val zip = app.repository.exportAllZip()
                        val target = app.platform.exportTargets.first { it != ExportTarget.SAVE_TO_DEVICE }
                        app.platform.export(target, zip, "PixelPals.zip", "application/zip") { ok ->
                            parentStatus =
                                if (ok) doneText else failText
                        }
                    }
                },
                onEmptyBin = { scope.launch { app.repository.emptyTrash() } },
                onReset = {
                    scope.launch {
                        app.repository.deleteEverything()
                        app.updateSettings { AppSettings() }
                        go(Route.Gallery)
                    }
                },
                onBack = {
                    parentStatus = null
                    go(Route.Gallery)
                },
            )
    }

    if (showNew) NewDrawingDialog(app.settings.allowCustomSizes, ::onChoice) { showNew = false }
    gate?.let { purpose ->
        ParentGateDialog(
            onPass = {
                gate = null
                when (purpose) {
                    GateFor.SETTINGS -> routeKey = Route.ParentSettings.encode()
                    GateFor.EMPTY_BIN ->
                        scope.launch {
                            app.repository.emptyTrash()
                            refresh++
                        }
                }
            },
            onDismiss = { gate = null },
        )
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(str(Res.string.corrupt_title)) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(str(Res.string.ok)) } },
        )
    }
}
