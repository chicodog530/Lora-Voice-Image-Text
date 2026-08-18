package com.example.loradigitalvoice

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.FormBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Esp32Status(
    val mode: String,
    val rxImageId: Long,
    val rxVoiceId: Long,
    val rxTextId: Long,
    val rxText: String,
    val imageRequested: Boolean
)

class Esp32Client(private val ipAddress: String = "192.168.4.1") {

    private val client = OkHttpClient()

    suspend fun getStatus(): Esp32Status? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("http://$ipAddress/status")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonString = response.body?.string() ?: return@use null
                    val json = JSONObject(jsonString)
                    Esp32Status(
                        mode = json.optString("mode", "idle"),
                        rxImageId = json.optLong("rxImageId", 0),
                        rxVoiceId = json.optLong("rxVoiceId", 0),
                        rxTextId = json.optLong("rxTextId", 0),
                        rxText = json.optString("rxText", ""),
                        imageRequested = json.optBoolean("imageRequested", false)
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadVoice(encodedVoiceData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "voice.c2",
                encodedVoiceData.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("http://$ipAddress/upload_voice")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadVoice(): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("http://$ipAddress/rx_voice.bin")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.bytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendText(text: String): Boolean = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder()
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url("http://$ipAddress/send_text")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadImage(imageData: ByteArray, width: Int, height: Int): Boolean = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "image.jpg",
                imageData.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("http://$ipAddress/upload")
            .header("X-Width", width.toString())
            .header("X-Height", height.toString())
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadImage(): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("http://$ipAddress/rx_image.bin")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.bytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun requestRemoteImage(): Boolean = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder().build() // Empty POST
        val request = Request.Builder()
            .url("http://$ipAddress/request_image")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
