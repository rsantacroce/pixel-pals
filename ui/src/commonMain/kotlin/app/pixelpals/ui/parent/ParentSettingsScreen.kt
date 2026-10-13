package app.pixelpals.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pixelpals.io.ThemeMode
import app.pixelpals.ui.AppController
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.KidConfirmDialog
import app.pixelpals.ui.common.KidTarget
import app.pixelpals.ui.common.SettingRow
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.back
import app.pixelpals.ui.res.cancel
import app.pixelpals.ui.res.empty_bin
import app.pixelpals.ui.res.empty_bin_confirm
import app.pixelpals.ui.res.export_all_zip
import app.pixelpals.ui.res.parent_settings
import app.pixelpals.ui.res.privacy_note
import app.pixelpals.ui.res.reset_app
import app.pixelpals.ui.res.reset_confirm
import app.pixelpals.ui.res.reset_yes
import app.pixelpals.ui.res.setting_custom_sizes
import app.pixelpals.ui.res.setting_labels
import app.pixelpals.ui.res.setting_sound
import app.pixelpals.ui.res.setting_theme
import app.pixelpals.ui.res.theme_dark
import app.pixelpals.ui.res.theme_light
import app.pixelpals.ui.res.theme_system
import app.pixelpals.ui.res.yes_delete
import app.pixelpals.ui.theme.PixelIcon

/** Settings reachable only through the parent gate. */
@Composable
fun ParentSettingsScreen(
    app: AppController,
    status: String?,
    onExportZip: () -> Unit,
    onEmptyBin: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val s = app.settings
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            KidButton(PixelIcon.HOME, str(Res.string.back), onBack)
            Text(
                str(Res.string.parent_settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingRow(str(Res.string.setting_sound)) {
                Switch(checked = app.soundOn, onCheckedChange = { on -> app.updateSettings { it.copy(soundOn = on) } })
            }
            SettingRow(str(Res.string.setting_labels)) {
                Switch(checked = s.showLabels, onCheckedChange = { on -> app.updateSettings { it.copy(showLabels = on) } })
            }
            SettingRow(str(Res.string.setting_custom_sizes)) {
                Switch(checked = s.allowCustomSizes, onCheckedChange = { on -> app.updateSettings { it.copy(allowCustomSizes = on) } })
            }
            SettingRow(str(Res.string.setting_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((mode, label) in listOf(
                        ThemeMode.SYSTEM to Res.string.theme_system,
                        ThemeMode.LIGHT to Res.string.theme_light,
                        ThemeMode.DARK to Res.string.theme_dark,
                    )) {
                        FilterChip(selected = s.theme == mode, onClick = {
                            app.updateSettings { it.copy(theme = mode) }
                        }, label = { Text(str(label)) })
                    }
                }
            }
            OutlinedButton(onClick = onExportZip, modifier = Modifier.fillMaxWidth().height(KidTarget)) {
                Text(str(Res.string.export_all_zip))
            }
            OutlinedButton(onClick = {
                confirmEmpty = true
            }, modifier = Modifier.fillMaxWidth().height(KidTarget)) { Text(str(Res.string.empty_bin)) }
            Button(
                onClick = { confirmReset = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(KidTarget),
            ) { Text(str(Res.string.reset_app)) }
            status?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            Text(str(Res.string.privacy_note), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
    if (confirmEmpty) {
        KidConfirmDialog(str(Res.string.empty_bin_confirm), str(Res.string.yes_delete), str(Res.string.cancel), onEmptyBin, {
            confirmEmpty =
                false
        })
    }
    if (confirmReset) {
        KidConfirmDialog(str(Res.string.reset_app), str(Res.string.reset_yes), str(Res.string.cancel), onReset, {
            confirmReset = false
        }, text = str(Res.string.reset_confirm))
    }
}
