package com.pha.document.scanner.common.document_scanner.listener

import com.pha.document.scanner.common.document_scanner.model.ErrorScannerModel
import com.pha.document.scanner.common.document_scanner.model.ResultScannedModel

interface DocumentReaderListener
{
    fun onResultScanned(result: ResultScannedModel)
    fun onFailed(error: ErrorScannerModel)
    
    fun scanSurfacePictureTaken()
    
    fun onShowFlash()
    fun onHideFlash()
    fun showFlashModeOn()
    fun showFlashModeOff()
}