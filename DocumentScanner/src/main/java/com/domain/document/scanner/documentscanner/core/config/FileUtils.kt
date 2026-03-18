package com.domain.document.scanner.documentscanner.core.config

import android.app.Activity
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun toFile(activity: Activity, bitmap: Bitmap, child: String, quality: Int = 90): File? {
        var file: File? = null
        return try {
            file = File(activity.externalCacheDirs[0], child)
            file.createNewFile()

            // convert bitmap to byte array
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val byteArray = stream.toByteArray()

            // write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(byteArray)
            fos.flush()
            fos.close()
            file
        } catch (_: Exception) {
            file
        }
    }
}