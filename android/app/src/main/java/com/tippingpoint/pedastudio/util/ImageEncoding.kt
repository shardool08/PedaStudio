package com.tippingpoint.pedastudio.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

object ImageEncoding {
    fun encodeBitmap(bitmap: Bitmap): Pair<String, String> {
        val scaled = if (bitmap.width > 1280) {
            val ratio = 1280f / bitmap.width
            bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt())
        } else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP) to "image/jpeg"
    }

    fun encodeUri(context: Context, uri: Uri): Pair<String, String> {
        val input = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot read image")
        val bytes = input.use { it.readBytes() }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Cannot decode image")
        return encodeBitmap(bitmap)
    }
}

data class EncodedImage(val base64: String, val mediaType: String)
