package com.example.loradigitalvoice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioController {
    private val sampleRate = 8000
    private val codec2Wrapper = Codec2Wrapper()
    private val esp32Client = Esp32Client()
    
    private var isRecording = false
    private var recordingJob: Job? = null
    
    // Store encoded voice here while recording
    private val encodedVoiceBuffer = mutableListOf<Byte>()

    init {
        codec2Wrapper.initCodec(1) // MODE_2400
    }

    @SuppressLint("MissingPermission")
    fun startRecording(context: Context) {
        if (isRecording) return
        isRecording = true
        encodedVoiceBuffer.clear()
        
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val bufferSize = maxOf(minBufferSize, 320 * 2) 
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val pcmBuffer = ShortArray(160) // 160 samples per Codec2 frame at 8000Hz = 20ms
            
            audioRecord.startRecording()
            Log.d("AudioController", "Started recording...")
            
            while (isRecording) {
                val readResult = audioRecord.read(pcmBuffer, 0, pcmBuffer.size)
                if (readResult == pcmBuffer.size) {
                    val encodedBytes = codec2Wrapper.encode(pcmBuffer)
                    if (encodedBytes != null) {
                        encodedVoiceBuffer.addAll(encodedBytes.toList())
                    }
                }
                
                // ESP32 buffer is exactly 12,288 bytes.
                // Stop at 12,000 bytes (30 seconds) to prevent silent truncation!
                if (encodedVoiceBuffer.size >= 12000) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "30 Second Limit Reached!", android.widget.Toast.LENGTH_LONG).show()
                        isRecording = false
                    }
                }
            }
            
            audioRecord.stop()
            audioRecord.release()
            
            Log.d("AudioController", "Stopped recording. Total encoded bytes: ${encodedVoiceBuffer.size}")
            uploadToEsp32(context)
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    private suspend fun uploadToEsp32(context: Context) {
        val bytesToUpload = encodedVoiceBuffer.toByteArray()
        Log.d("AudioController", "Uploading ${bytesToUpload.size} bytes to ESP32...")
        try {
            val success = esp32Client.uploadVoice(bytesToUpload)
            withContext(Dispatchers.Main) {
                if (success) {
                    Log.d("AudioController", "Upload successful!")
                    android.widget.Toast.makeText(context, "Voice Message Sent!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("AudioController", "Upload failed.")
                    android.widget.Toast.makeText(context, "ESP32 is Busy! Wait a moment and try again.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioController", "Upload error: ${e.message}")
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Network Error: Could not reach ESP32.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun downloadAndPlay(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("AudioController", "Downloading incoming voice from ESP32...")
            try {
                val encodedBytes = esp32Client.downloadVoice()
                if (encodedBytes != null && encodedBytes.isNotEmpty()) {
                    Log.d("AudioController", "Downloaded ${encodedBytes.size} bytes. Playing...")
                    playAudio(encodedBytes)
                } else {
                    Log.e("AudioController", "No voice data received.")
                }
            } catch (e: Exception) {
                Log.e("AudioController", "Download error: ${e.message}")
            }
        }
    }

    private fun playAudio(encodedBytes: ByteArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
            AudioTrack.MODE_STREAM
        )

        val decodedPcm = codec2Wrapper.decode(encodedBytes)
        if (decodedPcm != null) {
            audioTrack.play()
            audioTrack.write(decodedPcm, 0, decodedPcm.size)
        }
        
        audioTrack.stop()
        audioTrack.release()
    }
}
