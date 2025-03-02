package com.pha.document.scanner.common.documentscanner.ui.components.scansurface

import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel

internal interface ScanSurfaceListener
{
    fun scanSurfacePictureTaken()
    fun scanSurfaceShowProgress()
    fun scanSurfaceHideProgress()
    fun onError(error: ErrorScannerModel)
    
    fun showFlash()
    fun hideFlash()
    fun showFlashModeOn()
    fun showFlashModeOff()
}