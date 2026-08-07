package com.narcic.ng.handler

import com.narcic.ng.AppConfig
import com.narcic.ng.dto.entities.SubscriptionItem

/**
 * Ensures the app always has a subscription pointing to the Narcic NG
 * GitHub config repository. Only creates/updates the subscription entry —
 * the actual fetch is left to the app's own standard, already-tested
 * subscription update pipeline (MainAction.UpdateSubscriptions), so there
 * is only ever ONE code path that fetches and refreshes the server list.
 */
object DefaultConfigSource {

    /**
     * @return true if a fetch should be triggered afterwards (subscription
     * was just created or was disabled and got re-enabled).
     */
    fun ensureSubscriptionExists(): Boolean {
        val baseUrl = AppConfig.DEFAULT_SUBSCRIPTION_URL
        // Cache-buster: raw.githubusercontent.com CDN caches content for a
        // few minutes, so append a changing query param to always get fresh data.
        val fetchUrl = "$baseUrl?_=${System.currentTimeMillis()}"

        val subscriptions = MmkvManager.decodeSubscriptions()
        val existing = subscriptions.find { it.subscription.url.substringBefore("?") == baseUrl }

        if (existing != null) {
            val wasDisabled = !existing.subscription.enabled
            existing.subscription.url = fetchUrl
            existing.subscription.enabled = true
            MmkvManager.encodeSubscription(existing.guid, existing.subscription)
            return wasDisabled
        }

        val subItem = SubscriptionItem().apply {
            remarks = "Narcic NG"
            url = fetchUrl
            enabled = true
            autoUpdate = true
            updateInterval = 60
        }
        MmkvManager.encodeSubscription("", subItem)
        return true
    }
}
