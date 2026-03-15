package com.domain.document.scanner

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.domain.document.scanner.databinding.ActivityMainBinding
import com.domain.document.scanner.documentscanner.core.config.DocumentScannerConfig
import com.domain.document.scanner.documentscanner.core.config.ScanResultHandler
import com.domain.document.scanner.documentscanner.core.manager.DocumentScannerManager
import com.domain.document.scanner.documentscanner.core.options.DocumentScannerOptions
import com.domain.document.scanner.documentscanner.permission.PermissionUtil

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var documentScanner: DocumentScannerManager

    private val list: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onSetupView()
        initEventClick()
    }

    private fun onSetupView() {
        val config = DocumentScannerConfig.Builder()
            .setOutputFormat(DocumentScannerOptions.JPEG)
            .build()

        documentScanner = DocumentScannerManager(
            activity = this,
            lifecycle = this,
            viewFinder = binding.viewFinder,
            config = config
        )

        PermissionUtil.checkCameraSelfPermission(context = this) {
            documentScanner.startCamera()
        }
    }

    private fun initEventClick() {
        binding.capture.setOnClickListener {
            documentScanner.capture(result = object : ScanResultHandler {
                override fun onPageCaptured(path: String) {
                    list.add(element = path)
                    Toast.makeText(this@MainActivity, "$list", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtil.checkCameraGranted(this, requestCode) {
            documentScanner.startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        documentScanner.stopCamera()
    }
}