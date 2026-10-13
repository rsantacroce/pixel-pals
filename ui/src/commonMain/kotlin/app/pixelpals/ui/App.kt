package app.pixelpals.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.pixelpals.model.hello

@Composable
fun App() {
    MaterialTheme { Text(hello()) }
}
