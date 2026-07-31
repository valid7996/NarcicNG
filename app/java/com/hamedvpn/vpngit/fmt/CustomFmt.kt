package com.hamedvpn.vpngit.fmt

import com.hamedvpn.vpngit.dto.V2rayConfig
import com.hamedvpn.vpngit.dto.entities.ProfileItem
import com.hamedvpn.vpngit.enums.EConfigType
import com.hamedvpn.vpngit.util.JsonUtil

object CustomFmt : FmtBase() {
    
    fun parse(str: String): ProfileItem {
        val config = ProfileItem.create(EConfigType.CUSTOM)

        val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java)
        val outbound = fullConfig?.getProxyOutbound()

        config.remarks = fullConfig?.remarks ?: System.currentTimeMillis().toString()
        config.server = outbound?.getServerAddress()
        config.serverPort = outbound?.getServerPort()?.toString()

        return config
    }
}
