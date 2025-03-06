package com.pha.document.scanner.common.document_scanner.task

import com.google.mlkit.vision.objects.DetectedObject
import com.pha.document.scanner.common.document_scanner.util.DocDetectionTask
import com.pha.document.scanner.common.liveness_detection.utils.DetectionUtils

class ObjectingDetectionTask : DocumentDetectionTask
{
    companion object
    {
        private const val DOC_CAMERA_KEEP_TIME = 1500L
    }
    
    override var isTaskCompleted: Boolean = false
    
    override fun taskName(): String
    {
        return DocDetectionTask.DETECTION
    }
    
    override fun taskDescription(): String
    {
        return "Please squarely your document the camera"
    }
    
    private var startTime = 0L
    
    override fun start()
    {
        startTime = System.currentTimeMillis()
    }
    
    override fun process(doc: DetectedObject, timestamp: Long): Boolean
    {
        if (!DetectionUtils.isObjecting(doc))
        {
            startTime = System.currentTimeMillis()
            return false
        }
        return System.currentTimeMillis() - startTime >= DOC_CAMERA_KEEP_TIME
    }
}