package com.pha.document.scanner.common.documentscanner

import com.pha.document.scanner.common.documentscanner.ui.scan.BaseDocumentScannerActivity

abstract class BaseScannerActivity : BaseDocumentScannerActivity()
{
    fun addFragmentContentLayout()
    {
        addFragmentContentLayoutInternal()
    }
}