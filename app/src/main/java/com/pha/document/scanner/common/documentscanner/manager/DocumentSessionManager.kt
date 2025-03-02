package com.pha.document.scanner.common.documentscanner.manager

import android.content.Context
import android.graphics.Bitmap
import id.zelory.compressor.extension
import java.util.Locale

internal class DocumentSessionManager(context: Context)
{
    private val preferences = context.getSharedPreferences("DocumentScannerStore", Context.MODE_PRIVATE)
    
    companion object
    {
        private const val IMAGE_SIZE_KEY = "IMAGE_SIZE_KEY"
        private const val IMAGE_QUALITY_KEY = "IMAGE_QUALITY_KEY"
        private const val IMAGE_TYPE_KEY = "IMAGE_TYPE_KEY"
        
        private const val DEFAULT_IMAGE_TYPE = "jpg"
    }
    
    fun onGetImageSize(): Long = preferences.getLong(IMAGE_SIZE_KEY, -1L)
    
    fun onSetImageSize(size: Long) = preferences.edit().putLong(IMAGE_SIZE_KEY, size).apply()
    
    fun onGetImageQuality(): Int = preferences.getInt(IMAGE_QUALITY_KEY, 100)
    
    fun onSetImageQuality(quality: Int) = preferences.edit().putInt(IMAGE_QUALITY_KEY, quality).apply()
    
    fun onGetImageType(): Bitmap.CompressFormat = onCompressFormat(preferences.getString(IMAGE_TYPE_KEY, DEFAULT_IMAGE_TYPE) ?: "")
    
    fun onSetImageType(type: Bitmap.CompressFormat) = preferences.edit().putString(IMAGE_TYPE_KEY, type.extension()).apply()
    
    private fun onCompressFormat(format: String) = when (format.lowercase(Locale.ROOT))
    {
        "png" -> Bitmap.CompressFormat.PNG
        "webp" -> Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.JPEG
    }
}
