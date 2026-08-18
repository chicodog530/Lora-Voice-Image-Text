package com.example.loradigitalvoice.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
fun ImageScreen(
    rxImageData: ByteArray?,
    onUploadImage: suspend (ByteArray, Int, Int) -> Boolean,
    onRequestImage: suspend () -> Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSending by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("") }
    var halfSize by remember { mutableStateOf(false) }
    
    // Convert RX byte array to Bitmap for display
    val rxBitmap = remember(rxImageData) {
        if (rxImageData != null && rxImageData.isNotEmpty()) {
            BitmapFactory.decodeByteArray(rxImageData, 0, rxImageData.size)
        } else null
    }

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        selectedBitmap = bitmap
                        uploadStatus = "Image selected. Press Send to upload."
                    } else {
                        uploadStatus = "Failed to open image."
                    }
                } catch (e: Exception) {
                    uploadStatus = "Error: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
                onClick = { pickImageLauncher.launch("image/*") },
                enabled = !isSending,
                modifier = Modifier.weight(1f).height(60.dp)
            ) {
                Text("Select Image")
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

        if (selectedBitmap != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Selected Image Preview:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Image(
                bitmap = selectedBitmap!!.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier.size(150.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSending = true
                        uploadStatus = "Compressing..."
                        
                        val original = selectedBitmap!!
                        val targetWidth = if (halfSize) 400 else 800
                        val targetHeight = if (halfSize) 300 else 600

                        // Calculate aspect ratio preserving dimensions
                        val aspectRatio = original.width.toFloat() / original.height.toFloat()
                        var finalWidth = targetWidth
                        var finalHeight = (targetWidth / aspectRatio).toInt()
                        
                        if (finalHeight > targetHeight) {
                            finalHeight = targetHeight
                            finalWidth = (targetHeight * aspectRatio).toInt()
                        }

                        val bitmapToProcess = Bitmap.createScaledBitmap(original, finalWidth, finalHeight, true)
                        
                        // Compress bitmap to JPEG
                        val stream = ByteArrayOutputStream()
                        bitmapToProcess.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                        val jpegBytes = stream.toByteArray()
                        
                        uploadStatus = "Uploading ${jpegBytes.size} bytes..."
                        val success = onUploadImage(jpegBytes, finalWidth, finalHeight)
                        
                        if (success) {
                            uploadStatus = "Upload Successful!"
                            selectedBitmap = null // Clear preview
                        } else {
                            uploadStatus = "Upload Failed."
                        }
                        isSending = false
                    }
                },
                enabled = !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("SEND TO ESP32")
            }
        }
        
        if (uploadStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = uploadStatus)
        }
    }
}
