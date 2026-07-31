package com.hamedvpn.vpngit.service

import android.content.Context

interface IDialerService {
    fun start(context: Context, dialerAddr: String)
    fun stop()
}
