package com.skb.player.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skb.player.library.HistoryManager
import com.skb.player.library.MediaScanner
import com.skb.player.library.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SearchHit(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val source: String
)

@Composable
fun SearchScreen(
    history: HistoryManager,
    onBack: () -> Unit,
    onOpenVideo: (Uri) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var library by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        library = withContext(Dispatchers.IO) { MediaScanner.scanVideos(context, 500) }
    }

    val results = remember(query, library) {
        if (query.isBlank()) emptyList<SearchHit>()
        else {
            val q = query.lowercase().trim()
            val fromLib = library
                .filter { it.name.lowercase().contains(q) }
                .map { SearchHit(it.uri, it.name, it.durationMs, "Library") }
            val fromHist = history.getRecent(200)
                .filter { it.name.lowercase().contains(q) }
                .map { SearchHit(it.uri, it.name, it.durationMs, "History") }
            (fromLib + fromHist).distinctBy { it.uri }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("< Back") }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search videos") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        when {
            query.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Type to search your library", color = Color(0xFF888888))
                }
            }
            results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results for \"$query\"", color = Color(0xFF888888))
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${results.size} results",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888)
                        )
                    }
                    items(results, key = { it.uri.toString() }) { hit ->
                        SearchCard(hit) { onOpenVideo(hit.uri) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCard(hit: SearchHit, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                hit.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${fmtSearch(hit.durationMs)}  \u2022  ${hit.source}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A9A9A)
            )
        }
    }
}

private fun fmtSearch(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
