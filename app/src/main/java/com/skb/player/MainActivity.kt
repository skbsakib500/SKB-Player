package com.skb.player

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.skb.player.core.PlayerEngine
import com.skb.player.library.HistoryManager
import com.skb.player.ui.HomeScaffold
import com.skb.player.ui.VideoPlayerScreen

class MainActivity : ComponentActivity() {

    private lateinit var engine: PlayerEngine
    private lateinit var history: HistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = PlayerEngine(this)
        history = HistoryManager(this)

        setContent {
            MaterialTheme(colorScheme = skbAmoled()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var pickedUri by remember { mutableStateOf<Uri?>(null) }
                    var tab by remember { mutableIntStateOf(0) }
                    var lastBackAt by remember { mutableLongStateOf(0L) }

                    BackHandler {
                        when {
                            pickedUri != null -> pickedUri = null
                            tab != 0 -> tab = 0
                            else -> {
                                val now = System.currentTimeMillis()
                                if (now - lastBackAt < 2000L) {
                                    finish()
                                } else {
                                    lastBackAt = now
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Press back again to exit",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }

                    val picker = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()
                    ) { uri -> if (uri != null) pickedUri = uri }

                    val current = pickedUri
                    if (current == null) {
                        HomeScaffold(
                            history = history,
                            tab = tab,
                            onTabChange = { tab = it },
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

private fun skbAmoled() = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF0F0F0F),
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFEAEAEA),
    onSurface = Color(0xFFEAEAEA),
    onSurfaceVariant = Color(0xFFAAAAAA)
)
