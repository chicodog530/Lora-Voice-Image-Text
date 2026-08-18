package com.example.loradigitalvoice.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun ImageScreen(
    rxImageData: ByteArray?,
    onUploadImage: suspend (ByteArray, Int, Int) -> Boolean,
    onRequestImage: suspend () -> Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("") }
    var halfSize by remember { mutableStateOf(false) }
    
    // Convert RX byte array to Bitmap for display
    val rxBitmap = remember(rxImageData) {
        if (rxImageData != null && rxImageData.isNotEmpty()) {
            BitmapFactory.decodeByteArray(rxImageData, 0, rxImageData.size)
        } else null
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { originalBitmap ->
        if (originalBitmap != null) {
            coroutineScope.launch {
                isSending = true
                uploadStatus = "Compressing..."
                
                val bitmapToProcess = if (halfSize) {
                    Bitmap.createScaledBitmap(originalBitmap, originalBitmap.width / 2, originalBitmap.height / 2, true)
                } else {
                    originalBitmap
                }
                
                // Compress bitmap to JPEG
                val stream = ByteArrayOutputStream()
                bitmapToProcess.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                val jpegBytes = stream.toByteArray()
                
                uploadStatus = "Uploading ${jpegBytes.size} bytes..."
                val success = onUploadImage(jpegBytes, bitmapToProcess.width, bitmapToProcess.height)
                
                uploadStatus = if (success) "Upload Successful!" else "Upload Failed."
                isSending = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "LoRa Security Cam & Images", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Latest Received Image:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (rxBitmap != null) {
            Image(
                bitmap = rxBitmap.asImageBitmap(),
                contentDescription = "Received Image",
                modifier = Modifier.size(250.dp)
            )
        } else {
            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No image received yet.")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = halfSize,
                onCheckedChange = { halfSize = it }
            )
            Text(text = "Half Size Image (Faster LoRa Transfer)")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { takePictureLauncher.launch(null) },
                enabled = !isSending,
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("Send Photo")
            }
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSending = true
                        uploadStatus = "Requesting image..."
                        val success = onRequestImage()
                        uploadStatus = if (success) "Request Sent!" else "Request Failed."
                        isSending = false
                    }
                },
                enabled = !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("Request Remote")
            }
        }
        
        if (uploadStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = uploadStatus)
        }
    }
}
