package com.example.network

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RemoteAppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelogRu: String = "",
    val changelogEn: String = ""
)

sealed class AppUpdateCheckOutcome {
    data class UpdateAvailable(val info: RemoteAppVersionInfo) : AppUpdateCheckOutcome()
    data class UpToDate(val currentVersionName: String) : AppUpdateCheckOutcome()
    data object NetworkError : AppUpdateCheckOutcome()
}

object AppUpdateChecker {
    const val GITHUB_OWNER = "fatanime"
    const val GITHUB_REPO = "plump_heroes"

    val VERSION_JSON_CANDIDATE_URLS = listOf(
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/apk-distribution/version.json",
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/master/apk-distribution/version.json",
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/version.json"
    )

    fun isTrustedApkUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://")) return false
        return trimmed.startsWith("https://github.com/$GITHUB_OWNER/$GITHUB_REPO/") ||
            trimmed.startsWith("https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/") ||
            trimmed.startsWith("https://objects.githubusercontent.com/")
    }

    fun parseRemoteVersionJson(rawJson: String): RemoteAppVersionInfo? {
        if (rawJson.isBlank()) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val versionCode = obj.optInt("versionCode", -1)
            val versionName = obj.optString("versionName", "").trim()
            val apkUrl = obj.optString("apkUrl", "").trim()
            if (versionCode <= 0 || versionName.isBlank() || !isTrustedApkUrl(apkUrl)) {
                return null
            }
            val fallbackChangelog = obj.optString("changelog", "").trim()
            val changelogRu = obj.optString("changelogRu", fallbackChangelog).trim()
            val changelogEn = obj.optString("changelogEn", fallbackChangelog).trim()
            RemoteAppVersionInfo(
                versionCode = versionCode,
                versionName = versionName,
                apkUrl = apkUrl,
                changelogRu = changelogRu,
                changelogEn = changelogEn
            )
        }.getOrNull()
    }

    fun shouldNotifyUpdate(
        remote: RemoteAppVersionInfo,
        installedVersionCode: Int,
        skippedVersionCode: Int,
        ignoreSkipped: Boolean = false
    ): Boolean {
        if (remote.versionCode <= installedVersionCode) return false
        if (!ignoreSkipped && remote.versionCode == skippedVersionCode) return false
        return true
    }

    fun buildApkFileName(versionName: String): String {
        val sanitized = versionName
            .trim()
            .replace(" ", "_")
            .replace(Regex("[^A-Za-z0-9._-]"), "")
            .ifBlank { "1.0" }
        return "plump_heroes_${sanitized}.apk"
    }

    suspend fun checkRemoteVersion(
        installedVersionCode: Int,
        installedVersionName: String,
        skippedVersionCode: Int,
        ignoreSkipped: Boolean = false,
        httpFetcher: ((String) -> String?)? = null
    ): AppUpdateCheckOutcome = withContext(Dispatchers.IO) {
        if (httpFetcher == null && Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
            return@withContext AppUpdateCheckOutcome.NetworkError
        }

        val fetchFn = httpFetcher ?: ::fetchUrlText
        var parsedRemote: RemoteAppVersionInfo? = null
        for (url in VERSION_JSON_CANDIDATE_URLS) {
            val body = fetchFn(url) ?: continue
            val candidate = parseRemoteVersionJson(body)
            if (candidate != null) {
                parsedRemote = candidate
                break
            }
        }

        val remote = parsedRemote ?: return@withContext AppUpdateCheckOutcome.NetworkError
        if (shouldNotifyUpdate(remote, installedVersionCode, skippedVersionCode, ignoreSkipped)) {
            AppUpdateCheckOutcome.UpdateAvailable(remote)
        } else {
            AppUpdateCheckOutcome.UpToDate(installedVersionName)
        }
    }

    private fun fetchUrlText(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return runCatching {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        }.getOrNull().also {
            runCatching { conn?.disconnect() }
        }
    }
}
