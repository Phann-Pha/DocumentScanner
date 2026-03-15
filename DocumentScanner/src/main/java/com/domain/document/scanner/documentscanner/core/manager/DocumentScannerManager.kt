package com.domain.document.scanner.documentscanner.core.manager

import android.app.Activity
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.domain.document.scanner.documentscanner.core.config.DocumentScannerConfig
import com.domain.document.scanner.documentscanner.core.config.ScanResultHandler

class DocumentScannerManager(
    private val activity: Activity,
    private val lifecycle: LifecycleOwner,
    private val viewFinder: PreviewView,
    private val config: DocumentScannerConfig
) {

    private lateinit var session: DocumentScannerSession

    fun startCamera() {
        session = DocumentScannerSession(activity, lifecycle, viewFinder, config)
        session.start()
    }

    fun capture(result: ScanResultHandler) {
        session.capture(result)
    }

    fun stopCamera() {
        session.stop()
    }
}