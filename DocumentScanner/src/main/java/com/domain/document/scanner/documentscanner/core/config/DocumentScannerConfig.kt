package com.domain.document.scanner.documentscanner.core.config

import com.domain.document.scanner.documentscanner.core.options.DocumentScannerOptions
import java.io.File

class DocumentScannerConfig private constructor(
    val pages: Int,
    val resolution: Int,
    val format: DocumentScannerOptions,
    val directory: File?,
) {
    class Builder {
        private var pages: Int = 1
        private var resolution: Int = 100
        private var directory: File? = null
        private var format: DocumentScannerOptions = DocumentScannerOptions.JPEG

        fun setMaxPages(pages: Int) = apply {
            this.pages = pages
        }

        fun setResolution(resolution: Int) = apply {
            this.resolution = resolution
        }

        fun setOutputFormat(format: DocumentScannerOptions) = apply {
            this.format = format
        }

        fun setOutputDirectory(dir: File) = apply {
            this.directory = dir
        }

        fun build(): DocumentScannerConfig {
            return DocumentScannerConfig(
                pages = pages,
                format = format,
                resolution = resolution,
                directory = directory
            )
        }
    }
}