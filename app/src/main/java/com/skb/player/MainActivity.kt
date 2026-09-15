package com.skb.player

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.skb.player.core.PlayerEngine
import com.skb.player.library.HistoryManager
import com.skb.player.ui.HomeScreen
import com.skb.player.ui.VideoPlayerScreen

class MainActivity : ComponentActivity() {

    private lateinit var engine: PlayerEngine
    private lateinit var history: HistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = PlayerEngine(this)
        history = HistoryManager(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var pickedUri by remember { mutableStateOf<Uri?>(null) }

                    val picker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri -> if (uri != null) pickedUri = uri }

                    val current = pickedUri
                    if (current == null) {
                        HomeScreen(
                            history = history,
                            onOpenVideo = { pickedUri = it },
                            onPickVideo = { picker.launch(arrayOf("video/*")) }
                        )
                    } else {
                        VideoPlayerScreen(
                            player = engine.player,
                            uri = current,
                            history = history,
                            onBack = { pickedUri = null }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }
}
