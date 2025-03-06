package com.pha.document.scanner.common.document_scanner.task

import com.google.mlkit.vision.objects.DetectedObject

interface DocumentDetectionTask
{
    var isTaskCompleted: Boolean
    
    fun taskName(): String
    {
        return "Detection"
    }
    
    fun taskDescription(): String
    {
        return ""
    }
    
    fun isTaskPassed(): Boolean
    {
        return isTaskCompleted
    }
    
    fun start()
    {
    
    }
    
    /**
     * @return ture if task completed
     */
    fun process(face: DetectedObject, timestamp: Long): Boolean
}