package com.hamedvpn.vpngit.ui

import android.os.Bundle
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.core.CoreServiceManager

class ScSwitchActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moveTaskToBack(true)

        setContentView(R.layout.activity_none)

        if (CoreServiceManager.isRunning()) {
            CoreServiceManager.stopVService(this)
        } else {
            CoreServiceManager.startVServiceFromToggle(this)
        }
        finish()
    }
}

