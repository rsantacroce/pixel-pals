package app.pixelpals.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.pixelpals.ui.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Pixel Pals") { App() }
    }
