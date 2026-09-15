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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skb.player.library.HistoryManager
import com.skb.player.library.RecentEntry

@Composable
fun HistoryScreen(
    history: HistoryManager,
    onBack: () -> Unit,
    onOpenVideo: (Uri) -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val entries = remember(refresh) { history.getRecent(200) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("< Back") }
            Spacer(Modifier.padding(4.dp))
            Text(
                "History (${entries.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            if (entries.isNotEmpty()) {
                TextButton(onClick = {
                    history.clearAll()
                    refresh++
                }) { Text("Clear All", color = Color(0xFFFF6E6E)) }
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history yet", color = Color(0xFF888888))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.uri.toString() }) { entry ->
                    HistoryCard(
                        entry = entry,
                        onOpen = { onOpenVideo(entry.uri) },
                        onRemove = {
                            history.remove(entry.uri)
                            refresh++
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: RecentEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val pct = if (entry.durationMs > 0)
        (entry.positionMs * 100 / entry.durationMs).toInt().coerceIn(0, 100) else 0
    val frac = if (entry.durationMs > 0)
        entry.positionMs.toFloat() / entry.durationMs else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                entry.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$pct%  \u2022  ${fmtHistory(entry.positionMs)} / ${fmtHistory(entry.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A9A9A)
            )
            if (entry.watchMs > 0L) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "\u23F1 Watched: ${fmtHistory(entry.watchMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF00E5FF)
                )
            }
            LinearProgressIndicator(
                progress = { frac.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRemove) {
                    Text("Remove", color = Color(0xFFFF6E6E))
                }
            }
        }
    }
}

private fun fmtHistory(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
