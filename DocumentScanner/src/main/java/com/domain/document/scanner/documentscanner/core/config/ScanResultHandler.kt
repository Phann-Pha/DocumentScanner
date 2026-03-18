package com.domain.document.scanner.documentscanner.core.config

import java.io.File

interface ScanResultHandler {
    fun onPageCaptured(file: File) {
        // return file
    }
}