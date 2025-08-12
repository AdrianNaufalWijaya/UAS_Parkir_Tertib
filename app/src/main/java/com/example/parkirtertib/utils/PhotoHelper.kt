package com.example.parkirtertib.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Helper class untuk menangani operasi foto
 */
object PhotoHelper {

    private const val TAG = "PhotoHelper"
    private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
    private const val COMPRESSION_QUALITY_START = 100
    private const val COMPRESSION_QUALITY_STEP = 10
    private const val MIN_COMPRESSION_QUALITY = 10

    /**
     * Compress image dari Uri
     * @param context Context aplikasi
     * @param imageUri Uri gambar yang akan dikompres
     * @param maxSize Ukuran maksimal dalam bytes (default 1MB)
     * @return ByteArray hasil kompresi atau null jika gagal
     */
    fun compressImage(
        context: Context,
        imageUri: Uri,
        maxSize: Int = MAX_IMAGE_SIZE
    ): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from Uri")
                return null
            }

            // Fix orientation jika perlu
            val rotatedBitmap = fixImageOrientation(context, imageUri, bitmap)

            // Resize jika terlalu besar
            val resizedBitmap = resizeImageIfNeeded(rotatedBitmap, 1920, 1920)

            // Compress
            compressBitmap(resizedBitmap, maxSize)
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            null
        }
    }

    /**
     * Fix orientasi gambar berdasarkan EXIF data
     */
    private fun fixImageOrientation(
        context: Context,
        imageUri: Uri,
        bitmap: Bitmap
    ): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            }

            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing image orientation", e)
            bitmap
        }
    }

    /**
     * Resize gambar jika melebihi dimensi maksimal
     */
    private fun resizeImageIfNeeded(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    /**
     * Compress bitmap ke ByteArray dengan target size
     */
    private fun compressBitmap(
        bitmap: Bitmap,
        maxSize: Int
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        var quality = COMPRESSION_QUALITY_START

        // Compress dengan quality yang menurun sampai mencapai target size
        do {
            baos.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            quality -= COMPRESSION_QUALITY_STEP
        } while (baos.toByteArray().size > maxSize && quality > MIN_COMPRESSION_QUALITY)

        return baos.toByteArray()
    }

    /**
     * Save bitmap ke file temporary
     */
    fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "temp_photo_${System.currentTimeMillis()}.jpg"
    ): File? {
        return try {
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file", e)
            null
        }
    }

    /**
     * Get Uri dari Bitmap
     */
    fun getUriFromBitmap(
        context: Context,
        bitmap: Bitmap
    ): Uri? {
        return try {
            val file = saveBitmapToFile(context, bitmap)
            file?.let { Uri.fromFile(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Uri from bitmap", e)
            null
        }
    }

    /**
     * Delete temporary files dari cache
     */
    fun clearTempFiles(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles { file ->
                file.name.startsWith("temp_photo_")
            }
            files?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing temp files", e)
        }
    }

    /**
     * Check if image Uri is valid and accessible
     */
    fun isImageUriValid(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file size from Uri
     */
    fun getImageSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.available().toLong()
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Format file size untuk display
     */
    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            else -> String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0))
        }
    }
}