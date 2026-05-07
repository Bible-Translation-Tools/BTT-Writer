package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.downloading_index
import btt_writer.composeapp.generated.resources.mb_downloaded
import btt_writer.composeapp.generated.resources.out_of
import btt_writer.composeapp.generated.resources.pref_default_index_sqlite_url
import io.github.vinceglb.filekit.PlatformFile
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.inputStream
import org.bibletranslationtools.writer.network.HttpRequest
import org.jetbrains.compose.resources.getString

class ImportIndex(
    private val directoryProvider: DirectoryProvider,
    private val preference: Preference,
    private val catalogClient: ResourceCatalogClient
) {
    companion object {
        val TAG = ImportIndex::javaClass.name
    }

    @Suppress("DefaultLocale")
    suspend fun download(onProgress: (Float, String?) -> Unit = { _, _->}): Boolean {
        val message = getString(Res.string.downloading_index)

        onProgress(-1f, message)

        return try {
            catalogClient.closeLibrary()

            val url = preference.getPref(
                Preference.KEY_PREF_INDEX_SQLITE_URL,
                getString(Res.string.pref_default_index_sqlite_url)
            )

            val outOf = getString(Res.string.out_of)
            val mbDownloaded = getString(Res.string.mb_downloaded)

            HttpRequest.download(url, directoryProvider.databaseFile) { total, bytes ->
                if (total > 0) {
                    val message = String.format(
                        "%2.2f %s %2.2f %s",
                        bytes / (1024f * 1024f),
                        outOf,
                        total / (1024f * 1024f),
                        mbDownloaded
                    )
                    onProgress(bytes / total.toFloat(), message)
                }
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            catalogClient.openLibrary()
        }
    }

    fun import(index: PlatformFile): Boolean {
        return try {
            catalogClient.closeLibrary()

            index.inputStream().use { input ->
                directoryProvider.databaseFile.outputStream().use { output ->
                    val data = ByteArray(4096)
                    var total = 0
                    var count: Int
                    while ((input.read(data).also { count = it }) != -1) {
                        total += count
                        output.write(data, 0, count)
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to import index", e)
            false
        } finally {
            catalogClient.openLibrary()
        }
    }
}