package com.hamedvpn.vpngit.handler

import android.text.TextUtils
import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.dto.entities.SubscriptionCache
import com.hamedvpn.vpngit.dto.entities.SubscriptionItem
import com.hamedvpn.vpngit.util.LogUtil

object AutoConnectManager {

    fun ensureSubscription(): String {
        val baseUrl = AppConfig.DEFAULT_SUBSCRIPTION_URL
        val subUrl = "$baseUrl?_=${System.currentTimeMillis()}"
        LogUtil.i(AppConfig.TAG, "AutoConnectManager: Using subscription URL: $subUrl")

        // Find or create subscription (match by base URL, ignoring cache-buster)
        val subscriptions = MmkvManager.decodeSubscriptions()
        val existing = subscriptions.find { it.subscription.url.substringBefore("?") == baseUrl }

        val guid = if (existing != null) {
            existing.subscription.url = subUrl
            if (!existing.subscription.enabled) {
                existing.subscription.enabled = true
            }
            MmkvManager.encodeSubscription(existing.guid, existing.subscription)
            existing.guid
        } else {
            val subItem = SubscriptionItem().apply {
                remarks = "Narcic NG"
                url = subUrl
                enabled = true
                autoUpdate = true
                updateInterval = 60
            }
            MmkvManager.encodeSubscription("", subItem)
            val updatedSubs = MmkvManager.decodeSubscriptions()
            updatedSubs.lastOrNull()?.guid ?: return ""
        }

        // Always fetch fresh configs from the repo
        try {
            val subItem = MmkvManager.decodeSubscription(guid) ?: return guid
            val result = AngConfigManager.updateConfigViaSub(SubscriptionCache(guid, subItem))
            LogUtil.i(AppConfig.TAG, "AutoConnectManager: Fetch result - configCount=${result.configCount}, success=${result.successCount}, failure=${result.failureCount}, skip=${result.skipCount}")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "AutoConnectManager: Failed to fetch from repo", e)
        }

        return guid
    }

    fun refreshBatch(subId: String): List<String> {
        if (subId.isBlank()) return emptyList()

        val subItem = MmkvManager.decodeSubscription(subId) ?: return emptyList()
        if (!subItem.enabled) return emptyList()

        val servers = MmkvManager.decodeServerList(subId)
        LogUtil.i(AppConfig.TAG, "AutoConnectManager: refreshBatch for $subId found ${servers.size} servers")
        return servers
    }

    fun isPanelConfigured(): Boolean {
        // Always configured because we have default panel values
        return true
    }
}
