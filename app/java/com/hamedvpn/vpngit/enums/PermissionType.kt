package com.hamedvpn.vpngit.enums

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

enum class PermissionType {
    CAMERA {
        override fun getPermission(): String = Manifest.permission.CAMERA
    },

    POST_NOTIFICATIONS {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun getPermission(): String = Manifest.permission.POST_NOTIFICATIONS
    },

    ACCESS_LOCAL_NETWORK {
        @RequiresApi(36) // Android 16 (CINNAMON_BUN)
        override fun getPermission(): String = "android.permission.ACCESS_LOCAL_NETWORK"
    };

    abstract fun getPermission(): String

    fun getLabel(): String {
        return when (this) {
            CAMERA -> "Camera"
            POST_NOTIFICATIONS -> "Notification"
            ACCESS_LOCAL_NETWORK -> "Local Network"
        }
    }
}
