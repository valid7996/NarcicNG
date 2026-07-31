package com.hamedvpn.vpngit.contracts

import android.app.Service

interface ServiceControl {
    
    fun getService(): Service

    
    fun startService()

    
    fun stopService()

    
    fun vpnProtect(socket: Int): Boolean
}

