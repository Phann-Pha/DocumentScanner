package com.pha.document.scanner.common.document_scanner.analyzer

import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.ImageProxy
import androidx.camera.view.TransformExperimental
import androidx.core.os.ExecutorCompat
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.max

@TransformExperimental
class DocumentAnalyzer(detector: LiveDocumentDetector) : Analyzer
{
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()
    )
    
    private val delegate = MlAnalyzer(listOf(objectDetector), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, ExecutorCompat.create(Handler(Looper.getMainLooper()))) { result ->
        detector.process(result.getValue(objectDetector), detectionSize, result.timestamp)
    }
    
    private var detectionSize: Int = 640
    
    override fun analyze(image: ImageProxy)
    {
        detectionSize = max(image.width, image.height)
        delegate.analyze(image)
    }
    
    override fun getDefaultTargetResolution(): Size
    {
        return delegate.defaultTargetResolution
    }
    
    override fun getTargetCoordinateSystem(): Int
    {
        return delegate.targetCoordinateSystem
    }
    
    override fun updateTransform(matrix: Matrix?)
    {
        delegate.updateTransform(matrix)
    }
}