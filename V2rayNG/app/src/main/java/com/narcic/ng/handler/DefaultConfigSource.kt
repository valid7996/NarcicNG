package com.narcic.ng.handler

import com.narcic.ng.AppConfig
import com.narcic.ng.dto.entities.SubscriptionCache
import com.narcic.ng.dto.entities.SubscriptionItem
import com.narcic.ng.util.LogUtil

/**
 * Ensures the app always has a subscription pointing to the Narcic NG
 * GitHub config repository, and fetches fresh configs from it.
 * Safe, additive feature: does not touch VPN/service/native code.
 */
object DefaultConfigSource {

    fun ensureAndFetch() {
        try {
            val baseUrl = AppConfig.DEFAULT_SUBSCRIPTION_URL
            // Cache-buster: raw.githubusercontent.com CDN caches content for a
            // few minutes, so append a changing query param to always get fresh data.
            val fetchUrl = "$baseUrl?_=${System.currentTimeMillis()}"

            val subscriptions = MmkvManager.decodeSubscriptions()
            val existing = subscriptions.find { it.subscription.url.substringBefore("?") == baseUrl }

            val guid = if (existing != null) {
                existing.subscription.url = fetchUrl
                if (!existing.subscription.enabled) {
                    existing.subscription.enabled = true
                }
                MmkvManager.encodeSubscription(existing.guid, existing.subscription)
                existing.guid
            } else {
                val subItem = SubscriptionItem().apply {
                    remarks = "Narcic NG"
                    url = fetchUrl
                    enabled = true
                    autoUpdate = true
                    updateInterval = 60
                }
                MmkvManager.encodeSubscription("", subItem)
                MmkvManager.decodeSubscriptions().lastOrNull()?.guid ?: return
            }

            val subItem = MmkvManager.decodeSubscription(guid) ?: return
            val result = AngConfigManager.updateConfigViaSub(SubscriptionCache(guid, subItem))
            LogUtil.i(
                AppConfig.TAG,
                "DefaultConfigSource: fetched configCount=${result.configCount} " +
                    "success=${result.successCount} failure=${result.failureCount}"
            )
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "DefaultConfigSource: failed to fetch", e)
        }
    }
}
