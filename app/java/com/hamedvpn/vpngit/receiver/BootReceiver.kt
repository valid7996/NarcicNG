package com.hamedvpn.vpngit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.core.CoreServiceManager
import com.hamedvpn.vpngit.handler.MmkvManager
import com.hamedvpn.vpngit.handler.SubscriptionUpdater
import com.hamedvpn.vpngit.util.LogUtil

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        LogUtil.i(AppConfig.TAG, "BootReceiver received: ${intent?.action}")

        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            LogUtil.w(AppConfig.TAG, "BootReceiver: Invalid context or action")
            return
        }

        if (!MmkvManager.decodeStartOnBoot()) {
            LogUtil.i(AppConfig.TAG, "BootReceiver: Auto-start on boot is disabled")
            return
        }

        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            LogUtil.w(AppConfig.TAG, "BootReceiver: No server selected")
            return
        }

        LogUtil.i(AppConfig.TAG, "BootReceiver: Starting V2Ray service")
        CoreServiceManager.startVService(context)
        SubscriptionUpdater.sync(context)
    }
}

