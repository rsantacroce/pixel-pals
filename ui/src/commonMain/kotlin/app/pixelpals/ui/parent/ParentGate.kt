package app.pixelpals.ui.parent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.pixelpals.ui.common.KidTarget
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.cancel
import app.pixelpals.ui.res.enter
import app.pixelpals.ui.res.erase_digit
import app.pixelpals.ui.res.hold_me
import app.pixelpals.ui.res.parent_gate_or_hold
import app.pixelpals.ui.res.parent_gate_question
import app.pixelpals.ui.res.parent_gate_title
import app.pixelpals.ui.res.parent_gate_wrong
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Grown-ups only: answer a multiplication question on a keypad, or hold a button for 3 seconds.
 * Calls [onPass] when either succeeds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentGateDialog(onPass: () -> Unit, onDismiss: () -> Unit) {
    val a = remember { Random.nextInt(6, 10) }
    val b = remember { Random.nextInt(6, 10) }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val hold = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(Res.string.parent_gate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(str(Res.string.parent_gate_question, a, b), style = MaterialTheme.typography.headlineSmall)
                Text(answer.ifEmpty { " " }, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("gate-answer"))
                if (wrong) Text(str(Res.string.parent_gate_wrong), color = MaterialTheme.colorScheme.error)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 5,
                ) {
                    for (digit in listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)) {
                        OutlinedButton(
                            onClick = {
                                wrong = false
                                if (answer.length < 3) answer += digit
                            },
                            modifier = Modifier.size(KidTarget).testTag("gate-digit-$digit"),
                        ) { Text("$digit", style = MaterialTheme.typography.titleLarge) }
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        answer = answer.dropLast(1)
                    }, modifier = Modifier.height(KidTarget).align(Alignment.CenterStart)) {
                        Text(str(Res.string.erase_digit))
                    }
                    Button(
                        onClick = {
                            if (answer.toIntOrNull() == a * b) {
                                onPass()
                            } else {
                                wrong = true
                                answer = ""
                            }
                        },
                        modifier = Modifier.height(KidTarget).align(Alignment.CenterEnd).testTag("gate-enter"),
                    ) { Text(str(Res.string.enter)) }
                }
                Text(str(Res.string.parent_gate_or_hold), style = MaterialTheme.typography.bodyMedium)
                val holdLabel = str(Res.string.hold_me)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(KidTarget)
                            .semantics { contentDescription = holdLabel }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown()
                                    val job =
                                        scope.launch {
                                            hold.snapTo(0f)
                                            hold.animateTo(1f, tween(3000, easing = LinearEasing))
                                            onPass()
                                        }
                                    waitForUpOrCancellation()
                                    job.cancel()
                                    scope.launch { hold.snapTo(0f) }
                                }
                            },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LinearProgressIndicator(progress = { hold.value }, modifier = Modifier.fillMaxWidth().height(KidTarget))
                        Text(holdLabel, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.height(KidTarget)) { Text(str(Res.string.cancel)) } },
    )
}
