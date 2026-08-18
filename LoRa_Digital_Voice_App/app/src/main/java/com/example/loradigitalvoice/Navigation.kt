package com.example.loradigitalvoice

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.loradigitalvoice.ui.main.MainScreen
import com.example.loradigitalvoice.ui.text.TextScreen
import com.example.loradigitalvoice.ui.image.ImageScreen
import kotlinx.coroutines.delay

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val esp32Client = remember { Esp32Client() }
    val audioController = remember { AudioController() }
    
    var rxText by remember { mutableStateOf("") }
    var rxImageData by remember { mutableStateOf<ByteArray?>(null) }
    
    var lastRxImageId by remember { mutableLongStateOf(0L) }
    var lastRxVoiceId by remember { mutableLongStateOf(0L) }
    var lastRxTextId by remember { mutableLongStateOf(0L) }
    var lastImageRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            val status = esp32Client.getStatus()
            if (status != null) {
                if (status.rxTextId != lastRxTextId && status.rxTextId != 0L) {
                    rxText = status.rxText
                    lastRxTextId = status.rxTextId
                    selectedTab = 1
                    android.widget.Toast.makeText(context, "New Text Message!", android.widget.Toast.LENGTH_SHORT).show()
                }
                if (status.rxImageId != lastRxImageId && status.rxImageId != 0L) {
                    val img = esp32Client.downloadImage()
                    if (img != null) rxImageData = img
                    lastRxImageId = status.rxImageId
                    selectedTab = 2
                    android.widget.Toast.makeText(context, "New Image Received!", android.widget.Toast.LENGTH_SHORT).show()
                }
                if (status.rxVoiceId != lastRxVoiceId && status.rxVoiceId != 0L) {
                    audioController.downloadAndPlay(context)
                    lastRxVoiceId = status.rxVoiceId
                    selectedTab = 0
                    android.widget.Toast.makeText(context, "Playing New Voice Message!", android.widget.Toast.LENGTH_SHORT).show()
                }
                if (status.imageRequested && !lastImageRequested) {
                    selectedTab = 2
                    android.widget.Toast.makeText(context, "Remote Requested an Image! Please send one.", android.widget.Toast.LENGTH_LONG).show()
                }
                lastImageRequested = status.imageRequested
            }
            delay(2000)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Text("🎙️") },
                    label = { Text("Voice") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Text("✉️") },
                    label = { Text("Text") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Text("🖼️") },
                    label = { Text("Image") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues)
        when (selectedTab) {
            0 -> MainScreen(audioController = audioController, modifier = modifier)
            1 -> TextScreen(rxText = rxText, onSendText = { esp32Client.sendText(it) }, modifier = modifier)
            2 -> ImageScreen(
                rxImageData = rxImageData, 
                onUploadImage = { data, w, h -> esp32Client.uploadImage(data, w, h) }, 
                onRequestImage = { esp32Client.requestRemoteImage() },
                modifier = modifier
            )
        }
    }
}
