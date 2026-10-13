package app.pixelpals.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.pixelpals.io.JvmFileStore
import app.pixelpals.ui.AppController
import app.pixelpals.ui.PixelPalsApp
import app.pixelpals.ui.platform.AndroidPlatformServices
import kotlinx.coroutines.runBlocking
import java.io.File

/** Single activity. Saves pending work in [onStop] so nothing is lost if the process is killed afterwards. */
class MainActivity : ComponentActivity() {
    private lateinit var app: AppController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val services = AndroidPlatformServices(this, "$packageName.fileprovider")
        app = AppController(JvmFileStore(File(filesDir, "pixelpals")), services)
        setContent { PixelPalsApp(app) }
    }

    override fun onStop() {
        // A debounced autosave may still be pending; write it now (a small, fast local file write).
        runBlocking { app.flush() }
        super.onStop()
    }
}
