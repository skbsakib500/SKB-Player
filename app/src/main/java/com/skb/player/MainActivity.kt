package com.skb.player

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skb.player.core.PlayerEngine
import com.skb.player.ui.VideoPlayerScreen

class MainActivity : ComponentActivity() {

    private lateinit var engine: PlayerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = PlayerEngine(this)

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
                        HomeScreen(onPick = { picker.launch(arrayOf("video/*")) })
                    } else {
                        VideoPlayerScreen(
                            player = engine.player,
                            uri = current,
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

@Composable
private fun HomeScreen(onPick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SKB Player", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("v0.2 — Video Engine", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPick) {
            Text("Pick Video")
        }
    }
}
