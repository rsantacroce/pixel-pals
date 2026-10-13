package app.pixelpals.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.pixelpals.io.JvmFileStore
import app.pixelpals.io.desktopDataDir
import app.pixelpals.ui.AppController
import app.pixelpals.ui.PixelPalsApp
import app.pixelpals.ui.platform.DesktopPlatformServices
import kotlinx.coroutines.runBlocking
import java.awt.Dimension
import java.awt.Frame

fun main() {
    var frame: Frame? = null
    val app = AppController(JvmFileStore(desktopDataDir()), DesktopPlatformServices { frame })
    // Save on any exit path, including Cmd+Q and OS shutdown.
    Runtime.getRuntime().addShutdownHook(Thread { runCatching { app.autosaverFlushBlocking() } })
    application {
        val state = rememberWindowState(width = 1280.dp, height = 820.dp, position = WindowPosition.PlatformDefault)
        Window(
            onCloseRequest = {
                app.autosaverFlushBlocking()
                exitApplication()
            },
            state = state,
            title = "Pixel Pals",
            icon = painterResource("icon.png"),
        ) {
            frame = window
            window.minimumSize = Dimension(480, 560)
            PixelPalsApp(app)
        }
    }
}

private fun AppController.autosaverFlushBlocking() = runBlocking { flush() }
