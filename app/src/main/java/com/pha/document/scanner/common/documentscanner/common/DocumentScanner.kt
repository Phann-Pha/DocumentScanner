package com.pha.document.scanner.common.documentscanner.common

import android.content.Context
import android.graphics.Bitmap
import com.pha.document.scanner.common.documentscanner.manager.DocumentSessionManager

object DocumentScanner
{
    data class Configuration(
        var imageQuality: Int = 100,
        var imageSize: Long = -1,
        var imageType: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    )
    
    fun init(context: Context, configuration: Configuration = Configuration())
    {
        try
        {
            System.loadLibrary("opencv_java4")
            val session = DocumentSessionManager(context)
            if (configuration.imageQuality in 1 .. 100)
            {
                session.onSetImageQuality(configuration.imageQuality)
            }
            session.onSetImageSize(configuration.imageSize)
            session.onSetImageType(configuration.imageType)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }
}