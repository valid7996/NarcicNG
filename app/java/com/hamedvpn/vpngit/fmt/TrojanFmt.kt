package com.hamedvpn.vpngit.fmt

import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.dto.entities.ProfileItem
import com.hamedvpn.vpngit.enums.EConfigType
import com.hamedvpn.vpngit.enums.NetworkType
import com.hamedvpn.vpngit.extension.idnHost
import com.hamedvpn.vpngit.util.Utils
import java.net.URI

object TrojanFmt : FmtBase() {
    
    fun parse(str: String): ProfileItem {
        val config = ProfileItem.create(EConfigType.TROJAN)

        val uri = URI(Utils.fixIllegalUrl(str))
        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
        config.server = uri.idnHost
        config.serverPort = uri.port.toString()
        config.password = uri.userInfo

        if (uri.rawQuery.isNullOrEmpty()) {
            config.network = NetworkType.TCP.type
            config.security = AppConfig.TLS
            config.insecure = false
        } else {
            val queryParam = getQueryParam(uri)

            getItemFormQuery(config, queryParam)
            config.security = queryParam["security"] ?: AppConfig.TLS
        }

        return config
    }

    
    fun toUri(config: ProfileItem): String {
        val dicQuery = getQueryDic(config)

        return toUri(config, config.password, dicQuery)
    }
}
