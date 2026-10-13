package app.pixelpals.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pixelpals.io.TrashedProject
import app.pixelpals.ui.common.KidButton
import app.pixelpals.ui.common.PixelThumbnail
import app.pixelpals.ui.common.str
import app.pixelpals.ui.res.Res
import app.pixelpals.ui.res.back
import app.pixelpals.ui.res.empty_bin
import app.pixelpals.ui.res.recycle_bin
import app.pixelpals.ui.res.recycle_bin_empty
import app.pixelpals.ui.res.restore
import app.pixelpals.ui.theme.PixelIcon

/** Deleted drawings (kept 30 days). Kids can bring them back; emptying is behind the parent gate. */
@Composable
fun RecycleBinScreen(items: List<TrashedProject>, onRestore: (String) -> Unit, onEmpty: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            KidButton(PixelIcon.HOME, str(Res.string.back), onBack)
            Text(
                str(Res.string.recycle_bin),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (items.isNotEmpty()) KidButton(PixelIcon.TRASH, str(Res.string.empty_bin), onEmpty)
        }
        if (items.isEmpty()) {
            Text(
                str(Res.string.recycle_bin_empty),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items, key = { it.summary.id }) { t ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.aspectRatio(0.8f),
                ) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        PixelThumbnail(t.summary.thumbnail, Modifier.weight(1f).fillMaxWidth())
                        Text(t.summary.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                        KidButton(PixelIcon.UNDO, str(Res.string.restore), { onRestore(t.summary.id) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
