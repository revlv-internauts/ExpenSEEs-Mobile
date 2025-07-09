package com.example.expensees.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun createImageUri(context: Context): Uri? {
    return try {
        val storageDir = context.cacheDir
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("createImageUri", "Failed to create cache directory: ${storageDir.absolutePath}")
            return null
        }
        // Use a unique filename with timestamp to avoid conflicts
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFile = File(storageDir, "expense_image_$timeStamp.jpg")
        if (imageFile.createNewFile()) {
            Log.d("createImageUri", "File created: ${imageFile.absolutePath}")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            Log.d("createImageUri", "Generated URI: $uri")
            uri
        } else {
            Log.e("createImageUri", "Failed to create file: ${imageFile.absolutePath}")
            null
        }
    } catch (e: Exception) {
        Log.e("createImageUri", "Error creating image URI: ${e.message}", e)
        null
    }
}