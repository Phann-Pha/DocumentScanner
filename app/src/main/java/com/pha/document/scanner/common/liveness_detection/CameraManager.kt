package com.pha.document.scanner.common.liveness_detection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface.ROTATION_90
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.pha.document.scanner.common.liveness_detection.analyzer.FaceAnalyzer
import com.pha.document.scanner.common.liveness_detection.analyzer.LiveDetector
import com.pha.document.scanner.common.liveness_detection.tasks.DetectionTask
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val liveDetector: LiveDetector,
    private val viewFinder: PreviewView,
    private val guidelineView: TextView,
    private val callback: (task: String) -> Unit
)
{
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelectorOption = CameraSelector.LENS_FACING_FRONT
    private var cameraProvider: ProcessCameraProvider? = null
    
    private var imageCapture: ImageCapture? = null
    private lateinit var imageAnalyzer: ImageAnalysis
    
    init
    {
        createNewExecutor()
    }
    
    private fun createNewExecutor()
    {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    fun pause()
    {
        cameraProvider?.unbindAll()
    }
    
    fun start()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder()
                        .setTargetRotation(ROTATION_90)
                        .build()
                
                val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(cameraSelectorOption)
                        .build()
                
                imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                
                imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, analyzer()) }
                
                configuration(cameraProvider, cameraSelector)
            }, ContextCompat.getMainExecutor(activity)
        )
    }
    
    private fun configuration(cameraProvider: ProcessCameraProvider?, cameraSelector: CameraSelector)
    {
        try
        {
            cameraProvider?.unbindAll()
            camera?.cameraControl?.enableTorch(false)
            camera = cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer, imageCapture)
            preview?.surfaceProvider = viewFinder.surfaceProvider
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }
    
    @OptIn(TransformExperimental::class)
    private fun analyzer(): ImageAnalysis.Analyzer
    {
        return FaceAnalyzer(buildLiveDetector())
    }
    
    private fun buildLiveDetector(): LiveDetector
    {
        val listener = object : LiveDetector.Listener
        {
            override fun onTaskStarted(task: DetectionTask)
            {
                // nothings
            }
            
            override fun onTaskCompleted(task: DetectionTask, isLastTask: Boolean)
            {
                callback.invoke(task.taskName())
            }
            
            @SuppressLint("SetTextI18n")
            override fun onTaskFailed(task: DetectionTask, code: Int)
            {
                when (code)
                {
                    LiveDetector.ERROR_MULTI_FACES ->
                    {
                        guidelineView.text = "Please make sure there is only one face on the screen."
                    }
                    
                    LiveDetector.ERROR_NO_FACE ->
                    {
                        guidelineView.text = "Please make sure there is a face on the screen."
                    }
                    
                    LiveDetector.ERROR_OUT_OF_DETECTION_RECT ->
                    {
                        guidelineView.text = "Please make sure there is a face in the Rectangle."
                    }
                    
                    else ->
                    {
                        guidelineView.text = "${task.taskName()} Failed."
                    }
                }
            }
        }
        
        return liveDetector.also { it.setListener(listener) }
    }
    
    fun capturePhoto(context: Activity, status: String, callback: (String, File) -> Unit)
    {
        val fileName = status + UUID.randomUUID().toString()
        val child = "/$fileName.jpg"
        val outputFileOptions = if (Build.VERSION.SDK_INT >= 29)
        {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }
            ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
        }
        else
        {
            val outputFileDir = File(context.externalCacheDirs[0], child)
            ImageCapture.OutputFileOptions.Builder(outputFileDir).build()
        }
        
        imageCapture?.takePicture(outputFileOptions, getExecutor(context),
            object : ImageCapture.OnImageSavedCallback
            {
                override fun onImageSaved(output: ImageCapture.OutputFileResults)
                {
                    try
                    {
                        val mImageDir = getImageDir(context, child)
                        if (!mImageDir.exists())
                        {
                            mImageDir.mkdirs()
                        }
                        callback.invoke(fileName, mImageDir)
                        
                    }
                    catch (e: Exception)
                    {
                        e.printStackTrace()
                    }
                }
                
                override fun onError(e: ImageCaptureException)
                {
                    e.printStackTrace()
                }
            })
    }
    
    private fun getExecutor(context: Context): Executor
    {
        return ContextCompat.getMainExecutor(context)
    }
    
    fun getImageDir(context: Activity, child: String): File
    {
        return if (Build.VERSION.SDK_INT >= 29)
        {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), child)
        }
        else
        {
            moveImageFile(
                File(context.externalCacheDirs[0], child),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), child)
            )
            val fileDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), child)
            if (!fileDir.exists())
            {
                fileDir.mkdirs()
            }
            fileDir
        }
    }
    
    private fun moveImageFile(sourceFilePath: File, destinationFilePath: File)
    {
        try
        {
            if (!sourceFilePath.exists())
            {
                return
            }
            val source: FileChannel? = FileInputStream(sourceFilePath).channel
            val destination: FileChannel? = FileOutputStream(destinationFilePath).channel
            if (destination != null && source != null)
            {
                destination.transferFrom(source, 0, source.size())
            }
            source?.close()
            destination?.close()
        }
        catch (ex: Exception)
        {
            ex.printStackTrace()
        }
    }
}