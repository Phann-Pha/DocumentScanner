package com.pha.document.scanner.common.liveness_detection.tasks

import com.google.mlkit.vision.face.Face
import com.pha.document.scanner.common.liveness_detection.utils.DetectionUtils

class MouthOpenDetectionTask : DetectionTask
{
    override var isTaskCompleted: Boolean = false
    
    override fun taskName(): String
    {
        return "MouthOpenDetection"
    }
    
    override fun taskDescription(): String
    {
        return "Please open your mouth"
    }
    
    override fun process(face: Face, timestamp: Long): Boolean
    {
        return DetectionUtils.isFacing(face) && DetectionUtils.isMouthOpened(face)
    }
}