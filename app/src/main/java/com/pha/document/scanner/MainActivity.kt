package com.pha.document.scanner

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.pha.document.scanner.common.documentscanner.model.ErrorScannerModel
import com.pha.document.scanner.common.documentscanner.model.ScannerResults
import com.pha.document.scanner.common.documentscanner.ui.BaseDocumentScannerActivity
import com.pha.document.scanner.databinding.ActivityMainBinding

class MainActivity : BaseDocumentScannerActivity()
{
    private lateinit var activity: Activity
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        activity = this
        binding = DataBindingUtil.setContentView(activity, R.layout.activity_main)
        addFragmentContentLayout()
    }
    
    override fun onError(error: ErrorScannerModel)
    {
        Toast.makeText(activity, "${error.message}", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSuccess(scannerResults: ScannerResults)
    {
        binding.imagePreview.setImageURI(scannerResults.croppedImageFile?.toUri())
    }
    
    override fun onClose()
    {
        finish()
    }
}