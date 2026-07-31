package com.hamedvpn.vpngit.helper

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.enums.PermissionType
import com.hamedvpn.vpngit.extension.toast

class PermissionHelper(private val activity: AppCompatActivity) {
    private var permissionCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionCallback?.invoke(isGranted)
            permissionCallback = null
        }

    
    fun request(permissionType: PermissionType, onGranted: () -> Unit) {
        val permission = permissionType.getPermission()
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            permissionCallback = { isGranted ->
                if (isGranted) {
                    onGranted()
                } else {
                    val message = "${activity.getString(R.string.toast_permission_denied)}  ${permissionType.getLabel()}"
                    activity.toast(message)
                }
            }
            permissionLauncher.launch(permission)
        }
    }
}
