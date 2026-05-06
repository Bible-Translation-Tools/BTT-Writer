package org.bibletranslationtools.writer.usecases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.network.HttpRequest

class CheckForLatestRelease(
    private val preference: Preference,
    private val platform: Platform
) {
    data class Result(val release: Release?)

    val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun execute(): Result {

        var release: Release? = null

        val githubApiUrl = preference.getGithubRepoApi()
        val url = "$githubApiUrl/releases/latest"

        HttpRequest.get<ReleaseInfo>(url)?.let { releaseInfo ->
            val tagParts = releaseInfo.tagName.split("\\+".toRegex())

            if (tagParts.size == 2) {
                val build = tagParts[1].toInt()
                try {
                    if (build > platform.info.versionCode) {
                        releaseInfo.assets.firstOrNull()?.let { asset ->
                            val url = if (platform.isStoreVersion) {
                                "https://play.google.com/store/apps/details?id=org.bibletranslationtools.writer"
                            } else asset.browserDownloadUrl

                            release = Release(
                                name = releaseInfo.name,
                                downloadUrl = url,
                                downloadSize = asset.size,
                                build
                            )
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to fetch the package info", e)
                }
            }
        } ?: run {
            Logger.e(TAG, "Failed to fetch latest release info. ${HttpRequest.lastResponse?.message}")
        }

        return Result(release)
    }

    companion object {
        val TAG: String = CheckForLatestRelease::class.java.simpleName
    }

    data class Release(
        val name: String,
        val downloadUrl: String,
        val downloadSize: Int,
        val build: Int
    )
}

@Serializable
private data class ReleaseInfo(
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val assets: List<Asset>
)

@Serializable
private data class Asset(
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val size: Int
)