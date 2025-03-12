package com.pha.document.scanner.common.document_scanner.model

import java.io.File

data class ResultScannedModel(
    val rawImage: File? = null,
    val croppedImage: File? = null,
    val portraitImage: File? = null
)