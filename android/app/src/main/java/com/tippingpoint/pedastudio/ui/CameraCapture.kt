package com.tippingpoint.pedastudio.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.File

private const val SCAN_CAPTURE_FILE = "pedastudio_scan_capture.jpg"

fun createCameraImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Fixed cache path so the photo survives activity recreation after the system camera closes. */
fun scanCaptureFile(context: Context): File = File(context.cacheDir, SCAN_CAPTURE_FILE)

fun scanCaptureUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", scanCaptureFile(context))

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        when (uri.scheme?.lowercase()) {
            "file" -> uri.path?.let { BitmapFactory.decodeFile(it) }?.let(::toSoftwareBitmap)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }.let(::toSoftwareBitmap)
            } else {
                decodeViaStream(context, uri)
            }
        }
    } catch (_: Exception) {
        decodeViaStream(context, uri)
    }
}

private fun toSoftwareBitmap(bitmap: Bitmap): Bitmap {
    if (bitmap.config != Bitmap.Config.HARDWARE) return bitmap
    return bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
}

private fun decodeViaStream(context: Context, uri: Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)?.let(::toSoftwareBitmap)
    }
}

fun encodeBitmapForScan(bitmap: Bitmap, maxBase64Chars: Int = 4_500_000): Pair<String, String>? {
    val software = toSoftwareBitmap(bitmap)
    if (software.width <= 0 || software.height <= 0) return null
    val scaled = if (software.width > 1280) {
        val ratio = 1280f / software.width
        software.scale((software.width * ratio).toInt(), (software.height * ratio).toInt())
    } else {
        software
    }
    var quality = 85
    while (quality >= 45) {
        val out = ByteArrayOutputStream()
        if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)) return null
        val bytes = out.toByteArray()
        if (bytes.isEmpty()) return null
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (encoded.length <= maxBase64Chars) {
            return encoded to "image/jpeg"
        }
        quality -= 15
    }
    return null
}

fun encodeImageUriForScan(context: Context, uri: Uri): Pair<String, String>? {
    val bitmap = loadBitmapFromUri(context, uri) ?: return null
    return encodeBitmapForScan(bitmap)
}
