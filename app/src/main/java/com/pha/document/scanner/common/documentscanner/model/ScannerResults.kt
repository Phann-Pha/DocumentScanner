package com.pha.document.scanner.common.documentscanner.model

import java.io.File

data class ScannerResults (
    val originalImageFile: File? = null,
    val croppedImageFile: File? = null,
    val transformedImageFile: File? = null
)