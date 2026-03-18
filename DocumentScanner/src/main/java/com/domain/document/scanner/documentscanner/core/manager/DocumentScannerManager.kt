package com.domain.document.scanner.documentscanner.core.manager

import android.app.Activity
import android.view.View
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.domain.document.scanner.documentscanner.core.config.DocumentScannerConfig
import com.domain.document.scanner.documentscanner.core.config.ScanResultHandler

class DocumentScannerManager(
    private val activity: Activity,
    private val lifecycle: LifecycleOwner,
    private val rectangle: View? = null,
    private val viewFinder: PreviewView,
    private val config: DocumentScannerConfig
) {
    private lateinit var session: DocumentScannerSession

    fun startCamera() {
        session = DocumentScannerSession(activity, lifecycle, rectangle, viewFinder, config)
        session.start()
    }

    fun capture(page: Int = 0, result: ScanResultHandler) {
        session.capture(page, result)
    }

    fun stopCamera() {
        session.stop()
    }
}