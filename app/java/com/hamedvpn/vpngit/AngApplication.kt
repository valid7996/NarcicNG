package com.hamedvpn.vpngit

import android.content.Context
import android.util.Log
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.WorkManager
import com.tencent.mmkv.MMKV
import com.hamedvpn.vpngit.AppConfig.ANG_PACKAGE
import com.hamedvpn.vpngit.handler.SettingsManager

class AngApplication : MultiDexApplication() {
    companion object {
        lateinit var application: AngApplication
        private const val TAG = "AngApplication"
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        application = this
    }

    private val workManagerConfiguration: Configuration = Configuration.Builder()
        .setDefaultProcessName("${ANG_PACKAGE}:bg")
        .build()

    override fun onCreate() {
        super.onCreate()

        try {
            MMKV.initialize(this)
        } catch (e: Exception) {
            Log.e(TAG, "MMKV initialization failed", e)
        }

        try {
            WorkManager.initialize(this, workManagerConfiguration)
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager initialization failed", e)
        }

        try {
            SettingsManager.initApp(this)
        } catch (e: Exception) {
            Log.e(TAG, "SettingsManager.initApp failed", e)
        }

        try {
            SettingsManager.setNightMode()
        } catch (e: Exception) {
            Log.e(TAG, "setNightMode failed", e)
        }

        try {
            es.dmoral.toasty.Toasty.Config.getInstance()
                .setGravity(android.view.Gravity.BOTTOM, 0, 300)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Toasty config failed", e)
        }
    }
}
