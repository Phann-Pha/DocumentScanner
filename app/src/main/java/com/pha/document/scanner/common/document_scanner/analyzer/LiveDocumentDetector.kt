package com.pha.document.scanner.common.document_scanner.analyzer

import com.google.mlkit.vision.objects.DetectedObject
import com.pha.document.scanner.common.document_scanner.task.DocumentDetectionTask
import com.pha.document.scanner.common.liveness_detection.utils.DetectionUtils
import java.util.Deque
import java.util.LinkedList

class LiveDocumentDetector(vararg tasks: DocumentDetectionTask)
{
    companion object
    {
        private const val DOC_CACHE_SIZE = 5
        private const val NO_ERROR = -1
        const val ERROR_NO_DOC = 0
        const val ERROR_MULTI_DOC = 1
        const val ERROR_OUT_OF_DETECTION_RECT = 2
    }
    
    init
    {
        check(tasks.isNotEmpty()) { "no tasks" }
    }
    
    private val tasks = tasks.asList()
    private var taskIndex = 0
    private var lastTaskIndex = -1
    private var currentErrorState = NO_ERROR
    private val lastDocs: Deque<DetectedObject> = LinkedList()
    private var listener: Listener? = null
    
    fun process(faces: List<DetectedObject>?, detectionSize: Int, timestamp: Long)
    {
        val task = tasks.getOrNull(taskIndex) ?: return
        if (taskIndex != lastTaskIndex)
        {
            lastTaskIndex = taskIndex
            task.start()
            listener?.onTaskStarted(task)
        }
        
        val face = filter(task, faces, detectionSize) ?: return
        if (task.process(face, timestamp))
        {
            task.isTaskCompleted = true
            listener?.onTaskCompleted(task, taskIndex == tasks.lastIndex)
            taskIndex++
        }
    }
    
    fun setListener(listener: Listener?)
    {
        this.listener = listener
    }
    
    fun getTaskSize(): Int
    {
        return this.tasks.size
    }
    
    private fun getTasks(): List<DocumentDetectionTask>
    {
        return this.tasks
    }
    
    private fun reset()
    {
        taskIndex = 0
        lastTaskIndex = -1
        lastDocs.clear()
        getTasks().forEach { it.isTaskCompleted = false }
    }
    
    private fun filter(task: DocumentDetectionTask, docs: List<DetectedObject>?, detectionSize: Int): DetectedObject?
    {
        if (docs != null && docs.size > 1)
        {
            changeErrorState(task, ERROR_MULTI_DOC)
            reset()
            return null
        }
        
        if (docs.isNullOrEmpty() && lastDocs.isEmpty())
        {
            changeErrorState(task, ERROR_NO_DOC)
            reset()
            return null
        }
        
        val face = docs?.firstOrNull() ?: lastDocs.pollFirst()
        if (!DetectionUtils.isObjectInDetectionRect(face, detectionSize))
        {
            changeErrorState(task, ERROR_OUT_OF_DETECTION_RECT)
            reset()
            return null
        }
        
        if (!docs.isNullOrEmpty())
        {
            // cache current face
            lastDocs.offerFirst(face)
            if (lastDocs.size > DOC_CACHE_SIZE)
            {
                lastDocs.pollLast()
            }
        }
        changeErrorState(task, NO_ERROR)
        return face
    }
    
    private fun changeErrorState(task: DocumentDetectionTask, newErrorState: Int)
    {
        if (newErrorState != currentErrorState)
        {
            currentErrorState = newErrorState
            if (currentErrorState != NO_ERROR)
            {
                listener?.onTaskFailed(task, currentErrorState)
            }
        }
    }
    
    interface Listener
    {
        fun onTaskStarted(task: DocumentDetectionTask)
        
        fun onTaskCompleted(task: DocumentDetectionTask, isLastTask: Boolean)
        
        fun onTaskFailed(task: DocumentDetectionTask, code: Int)
    }
}