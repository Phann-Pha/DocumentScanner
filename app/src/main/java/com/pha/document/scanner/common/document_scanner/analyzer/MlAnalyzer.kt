package com.pha.document.scanner.common.document_scanner.analyzer

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.TransformUtils
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.core.util.Consumer
import com.google.android.gms.common.internal.Preconditions
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.interfaces.Detector
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor

@TransformExperimental
class MlAnalyzer(detectors: List<Detector<*>>, targetCoordinateSystem: Int, executor: Executor, consumer: Consumer<Result>) : ImageAnalysis.Analyzer
{
    private val mDetectors: List<Detector<*>>
    private val mTargetCoordinateSystem: Int
    
    private val mConsumer: Consumer<Result>
    
    private val mImageAnalysisTransformFactory: ImageProxyTransformFactory
    private val mExecutor: Executor
    
    private var mSensorToTarget: Matrix? = null
    
    init
    {
        if (targetCoordinateSystem != ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL)
        {
            for (detector in detectors)
            {
                Preconditions.checkArgument(detector.detectorType != Detector.TYPE_SEGMENTATION, "Segmentation only works with COORDINATE_SYSTEM_ORIGINAL")
            }
        }
        
        mDetectors = ArrayList(detectors)
        mTargetCoordinateSystem = targetCoordinateSystem
        mConsumer = consumer
        mExecutor = executor
        mImageAnalysisTransformFactory = ImageProxyTransformFactory()
        mImageAnalysisTransformFactory.isUsingRotationDegrees = true
    }
    
    @SuppressLint("RestrictedApi")
    override fun analyze(imageProxy: ImageProxy)
    {
        val analysisToTarget = Matrix()
        if (mTargetCoordinateSystem != ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL)
        {
            val sensorToTarget = mSensorToTarget
            if (mTargetCoordinateSystem != ImageAnalysis.COORDINATE_SYSTEM_SENSOR && sensorToTarget == null)
            {
                imageProxy.close()
                return
            }
            val sensorToAnalysis = Matrix(imageProxy.imageInfo.sensorToBufferTransformMatrix)
            
            val sourceRect = RectF(0f, 0f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            val bufferRect = TransformUtils.rotateRect(sourceRect, imageProxy.imageInfo.rotationDegrees)
            val analysisToMlKitRotation = TransformUtils.getRectToRect(sourceRect, bufferRect, imageProxy.imageInfo.rotationDegrees)
            
            sensorToAnalysis.postConcat(analysisToMlKitRotation)
            sensorToAnalysis.invert(analysisToTarget)
            if (mTargetCoordinateSystem != ImageAnalysis.COORDINATE_SYSTEM_SENSOR)
            {
                analysisToTarget.postConcat(sensorToTarget)
            }
        }
        
        // Detect the image recursively, starting from index 0.
        detectRecursively(imageProxy, 0, analysisToTarget, HashMap(), HashMap())
    }
    
    @OptIn(ExperimentalGetImage::class)
    private fun detectRecursively(imageProxy: ImageProxy, detectorIndex: Int, transform: Matrix, values: MutableMap<Detector<*>, Any>, throwable: MutableMap<Detector<*>, Throwable?>)
    {
        val image = imageProxy.image
        if (image == null)
        {
            imageProxy.close()
            return
        }
        
        if (detectorIndex > mDetectors.size - 1)
        {
            imageProxy.close()
            mExecutor.execute {
                mConsumer.accept(Result(values, imageProxy.imageInfo.timestamp, throwable))
            }
            return
        }
        
        val detector = mDetectors[detectorIndex]
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val mlKitTask: Task<Any>
        
        try
        {
            mlKitTask = detector.process(image, rotationDegrees, transform) as Task<Any>
        }
        catch (e: Exception)
        {
            throwable[detector] = RuntimeException("Failed to process the image.", e)
            detectRecursively(imageProxy, detectorIndex + 1, transform, values, throwable)
            return
        }
        
        mlKitTask.addOnCompleteListener(mExecutor) { task: Task<Any> ->
            if (task.isCanceled)
            {
                throwable[detector] = CancellationException("The task is canceled.")
            }
            else if (task.isSuccessful)
            {
                values[detector] = task.result
            }
            else
            {
                throwable[detector] = task.exception
            }
            
            detectRecursively(imageProxy, detectorIndex + 1, transform, values, throwable)
        }
    }
    
    override fun getDefaultTargetResolution(): Size
    {
        var size: Size = DEFAULT_SIZE
        for (detector in mDetectors)
        {
            val detectorSize = getTargetResolution(detector.detectorType)
            if (detectorSize.height * detectorSize.width > size.width * size.height)
            {
                size = detectorSize
            }
        }
        return size
    }
    
    private fun getTargetResolution(detectorType: Int): Size
    {
        return when (detectorType)
        {
            Detector.TYPE_BARCODE_SCANNING, Detector.TYPE_TEXT_RECOGNITION -> Size(1280, 720)
            else -> DEFAULT_SIZE
        }
    }
    
    override fun getTargetCoordinateSystem(): Int
    {
        return mTargetCoordinateSystem
    }
    
    override fun updateTransform(matrix: Matrix?)
    {
        mSensorToTarget = if (matrix == null)
        {
            null
        }
        else
        {
            Matrix(matrix)
        }
    }
    
    class Result(private val mValues: MutableMap<Detector<*>, Any>, val timestamp: Long, private val mThrowable: MutableMap<Detector<*>, Throwable?>)
    {
        fun <T> getValue(detector: Detector<T>): T?
        {
            checkDetectorExists(detector)
            return mValues[detector] as T?
        }
        fun getThrowable(detector: Detector<*>): Throwable?
        {
            checkDetectorExists(detector)
            return mThrowable[detector]
        }
        private fun checkDetectorExists(detector: Detector<*>)
        {
            Preconditions.checkArgument(mValues.containsKey(detector) || mThrowable.containsKey(detector), "The detector does not exist")
        }
    }
    
    companion object
    {
        private val DEFAULT_SIZE = Size(480, 360)
    }
}
