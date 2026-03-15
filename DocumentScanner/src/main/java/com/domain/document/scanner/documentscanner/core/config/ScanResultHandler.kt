package com.domain.document.scanner.documentscanner.core.config

interface ScanResultHandler {

    fun onPageCaptured(path: String) {

    }
}