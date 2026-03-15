package com.domain.document.scanner.documentscanner.core.manager

import android.app.Activity
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.domain.document.scanner.documentscanner.core.config.DocumentScannerConfig
import com.domain.document.scanner.documentscanner.core.config.ScanResultHandler
import com.domain.document.scanner.documentscanner.exceptions.DocumentScannerException
import java.io.File
import java.util.concurrent.Executor

class DocumentScannerSession internal constructor(
    private val activity: Activity,
    private val lifecycle: LifecycleOwner,
    private val viewFinder: PreviewView,
    private val config: DocumentScannerConfig
) {

    private var preview: Preview? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var imageCapture: ImageCapture? = null

    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .build()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            config(cameraProvider, selector, imageCapture)
        }, executor(activity))
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }

    /** func for camera configuration like bind view */
    private fun config(provider: ProcessCameraProvider?, selector: CameraSelector, capture: ImageCapture?) {
        try {
            provider?.unbindAll()
            camera?.cameraControl?.enableTorch(false)
            camera = provider?.bindToLifecycle(lifecycle, selector, preview, capture)
            preview?.surfaceProvider = viewFinder.surfaceProvider
        } catch (_: Exception) {
            throw DocumentScannerException(message = "Error Camera Config")
        }
    }

    private fun executor(activity: Activity): Executor {
        return ContextCompat.getMainExecutor(activity)
    }

    fun capture(result: ScanResultHandler) {
        val format = config.format.name.lowercase()
        val child = "doc-image.$format"

        val file = config.directory ?: File(activity.externalCacheDirs[0], child)
        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture?.takePicture(output, executor(activity), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    result.onPageCaptured(path = output.savedUri?.path ?: "")
                } catch (_: Exception) {
                    throw DocumentScannerException(message = "Error Save Image")
                }
            }

            override fun onError(e: ImageCaptureException) {
                throw DocumentScannerException(message = "Error Capture Image")
            }
        })
    }
}