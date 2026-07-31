package com.hamedvpn.vpngit.ui

import android.content.Intent
import android.os.Bundle
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.extension.toastError
import com.hamedvpn.vpngit.extension.toastSuccess
import com.hamedvpn.vpngit.handler.AngConfigManager

class ScScannerActivity : HelperBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_none)
        importQRcode()
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) {
                val (count, countSub) = AngConfigManager.importBatchConfig(scanResult, "", false)

                if (count + countSub > 0) {
                    toastSuccess(R.string.toast_success)
                } else {
                    toastError(R.string.toast_failure)
                }

                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }
    }
}
