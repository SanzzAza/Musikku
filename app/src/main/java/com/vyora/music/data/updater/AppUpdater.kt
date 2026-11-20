package com.vyora.music.data.updater

import com.google.gson.JsonParser
import com.vyora.music.data.ytmusic.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class ReleaseUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val htmlUrl: String,
    val isNewer: Boolean,
)

/**
 * Pemeriksa pembaruan rilis otomatis VYORA langsung dari GitHub Releases publik
 * (100% Guest, tanpa perlu token/akun).
 */
object AppUpdater {

    private const val RELEASES_API = "https://api.github.com/repos/SanzzAza/Musikku/releases/latest"

    suspend fun checkLatest(currentVersion: String): Result<ReleaseUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "VYORA-Android-Updater")
                .build()

            StreamResolver.httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("GitHub API merespons HTTP ${resp.code}")
                }
                val root = JsonParser.parseString(resp.body?.string() ?: "{}").asJsonObject
                val tag = root.get("tag_name")?.asString.orEmpty().removePrefix("v").trim()
                val title = root.get("name")?.takeIf { !it.isJsonNull }?.asString?.ifBlank { "VYORA v$tag" } ?: "VYORA v$tag"
                val notes = root.get("body")?.takeIf { !it.isJsonNull }?.asString.orEmpty().trim()
                val htmlUrl = root.get("html_url")?.asString ?: "https://github.com/SanzzAza/Musikku/releases/latest"

                var apkUrl = htmlUrl
                val assets = root.getAsJsonArray("assets")
                if (assets != null) {
                    for (el in assets) {
                        val obj = el.asJsonObject
                        val name = obj.get("name")?.asString.orEmpty()
                        val dl = obj.get("browser_download_url")?.asString.orEmpty()
                        if (name.endsWith(".apk", ignoreCase = true) && dl.isNotBlank()) {
                            apkUrl = dl
                            break
                        }
                    }
                }

                ReleaseUpdateInfo(
                    latestVersion = tag.ifBlank { currentVersion },
                    releaseTitle = title,
                    changelog = notes,
                    apkDownloadUrl = apkUrl,
                    htmlUrl = htmlUrl,
                    isNewer = isRemoteNewer(currentVersion, tag),
                )
            }
        }
    }

    internal fun isRemoteNewer(installed: String, remote: String): Boolean {
        val a = installed.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
        val b = remote.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
        if (b.isEmpty()) return false
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (bv > av) return true
            if (bv < av) return false
        }
        return false
    }
}
