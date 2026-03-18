package com.domain.document.scanner.documentscanner.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {
    fun checkCameraSelfPermission(context: Activity, startCamera: () -> (Unit)) {
        if (allPermissionsGranted(context)) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(context, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    fun checkCameraGranted(context: Activity, requestCode: Int, startCamera: () -> (Unit)) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted(context)) {
                startCamera()
            }
        }
    }

    private fun allPermissionsGranted(context: Activity) = REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}