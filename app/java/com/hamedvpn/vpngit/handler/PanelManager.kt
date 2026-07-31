package com.hamedvpn.vpngit.handler

import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.BuildConfig
import com.hamedvpn.vpngit.dto.CheckUpdateResult
import com.hamedvpn.vpngit.dto.UrlContentRequest
import com.hamedvpn.vpngit.util.HttpUtil
import com.hamedvpn.vpngit.util.JsonUtil
import com.hamedvpn.vpngit.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object PanelManager {

    private fun getPanelUrl(): String? {
        val url = MmkvManager.decodeSettingsString(AppConfig.PREF_PANEL_URL)
        return if (url.isNullOrBlank()) AppConfig.DEFAULT_PANEL_URL else url.trimEnd('/')
    }

    private fun getApiKey(): String? {
        val key = MmkvManager.decodeSettingsString(AppConfig.PREF_PANEL_API_KEY)
        return if (key.isNullOrBlank()) AppConfig.DEFAULT_PANEL_API_KEY else key
    }

    fun isPanelConfigured(): Boolean {
        return true
    }

    suspend fun checkForUpdateFromPanel(): CheckUpdateResult? = withContext(Dispatchers.IO) {
        val panelUrl = getPanelUrl() ?: return@withContext null
        val apiKey = getApiKey() ?: return@withContext null

        try {
            val url = "$panelUrl/api/check-update?version=${BuildConfig.VERSION_NAME}"
            val response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 10000
                )
            ) ?: return@withContext null

            val json = JsonUtil.fromJsonSafe(response, Map::class.java) ?: return@withContext null

            val updateAvailable = json["update_available"] as? Boolean ?: false
            if (!updateAvailable) {
                return@withContext CheckUpdateResult(hasUpdate = false)
            }

            val version = json["version"] as? String
            val downloadUrl = json["download_url"] as? String
            val releaseNotes = json["release_notes"] as? String
            val isMandatory = json["is_mandatory"] as? Boolean ?: false
            val fileSize = (json["file_size"] as? Number)?.toLong() ?: 0L
            val fileHash = json["file_hash"] as? String

            return@withContext CheckUpdateResult(
                hasUpdate = true,
                latestVersion = version,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                isPreRelease = false
            )
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to check update from panel", e)
            return@withContext null
        }
    }

    suspend fun checkMaintenanceMode(): Boolean = withContext(Dispatchers.IO) {
        val panelUrl = getPanelUrl() ?: return@withContext false

        try {
            val url = "$panelUrl/api/settings"
            val response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 5000
                )
            ) ?: return@withContext false

            val json = JsonUtil.fromJsonSafe(response, Map::class.java) ?: return@withContext false
            return@withContext json["maintenance"] as? Boolean ?: false
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to check maintenance mode from panel", e)
            return@withContext false
        }
    }

    suspend fun reportConnection(serverId: Int) = withContext(Dispatchers.IO) {
        val panelUrl = getPanelUrl() ?: return@withContext

        try {
            val url = "$panelUrl/api/report"
            val body = JsonUtil.toJson(mapOf(
                "server_id" to serverId,
                "app_version" to BuildConfig.VERSION_NAME
            ))

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Connection", "close")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    LogUtil.i(AppConfig.TAG, "Reported connection to panel")
                } else {
                    LogUtil.w(AppConfig.TAG, "Failed to report connection: ${response.code}")
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to report connection to panel", e)
        }
    }
}
