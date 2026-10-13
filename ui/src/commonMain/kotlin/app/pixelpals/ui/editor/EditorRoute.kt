package app.pixelpals.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.pixelpals.io.CorruptProjectException
import app.pixelpals.model.EditorEffect
import app.pixelpals.ui.AppController
import app.pixelpals.ui.platform.Sound
import kotlinx.coroutines.launch

/** Loads a drawing, wires feedback (sounds, haptics) and shows the editor. */
@Composable
fun EditorRoute(
    app: AppController,
    projectId: String,
    onOpened: (EditorController?) -> Unit,
    onFailed: (corrupt: Boolean) -> Unit,
    onHome: () -> Unit,
) {
    var controller by remember(projectId) { mutableStateOf<EditorController?>(null) }
    var lockedTrigger by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val feedback by rememberUpdatedState<(Set<EditorEffect>) -> Unit>({ effects ->
        for (e in effects) {
            when (e) {
                EditorEffect.POP -> app.play(Sound.POP)
                EditorEffect.SPLASH -> {
                    app.play(Sound.SPLASH)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                EditorEffect.WHOOSH -> app.play(Sound.WHOOSH)
                EditorEffect.WIPE -> app.play(Sound.WIPE)
                EditorEffect.PICK -> app.play(Sound.CLICK)
                EditorEffect.TOOL -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                EditorEffect.BLOCKED -> lockedTrigger++
            }
        }
    })
    LaunchedEffect(projectId) {
        try {
            val project = app.repository.load(projectId)
            controller = EditorController(project, app.autosaver) { feedback(it) }.also(onOpened)
        } catch (e: CorruptProjectException) {
            onFailed(true)
        } catch (e: Exception) {
            onFailed(false)
        }
    }
    DisposableEffect(projectId) {
        onDispose {
            onOpened(null)
            app.scope.launch { app.flush() }
        }
    }
    val c = controller
    if (c == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        EditorScreen(app, c, rememberLockedHint(lockedTrigger), onHome)
    }
}
