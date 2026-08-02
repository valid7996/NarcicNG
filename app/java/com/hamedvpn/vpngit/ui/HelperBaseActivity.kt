package com.hamedvpn.vpngit.ui

import android.net.Uri
import android.os.Bundle
import com.hamedvpn.vpngit.enums.PermissionType
import com.hamedvpn.vpngit.helper.FileChooserHelper
import com.hamedvpn.vpngit.helper.PermissionHelper
import com.hamedvpn.vpngit.helper.QRCodeScannerHelper

abstract class HelperBaseActivity : BaseActivity() {
    private lateinit var fileChooser: FileChooserHelper
    private lateinit var permissionRequester: PermissionHelper
    private lateinit var qrCodeScanner: QRCodeScannerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileChooser = FileChooserHelper(this)
        permissionRequester = PermissionHelper(this)
        qrCodeScanner = QRCodeScannerHelper(this)
    }

    protected fun checkAndRequestPermission(
        permissionType: PermissionType,
        onGranted: () -> Unit
    ) {
        permissionRequester.request(permissionType, onGranted)
    }

    protected fun launchFileChooser(
        mimeType: String = "*/*",
        onResult: (Uri?) -> Unit
    ) {
        fileChooser.launch(mimeType, onResult)
    }

    protected fun launchCreateDocument(
        fileName: String,
        onResult: (Uri?) -> Unit
    ) {
        fileChooser.createDocument(fileName, onResult)
    }

    protected fun launchQRCodeScanner(onResult: (String?) -> Unit) {
        checkAndRequestPermission(PermissionType.CAMERA) {
            qrCodeScanner.launch(onResult)
        }
    }
}
