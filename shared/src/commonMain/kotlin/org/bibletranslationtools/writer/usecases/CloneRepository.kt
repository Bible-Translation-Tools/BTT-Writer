package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.downloading
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.git.TransportCallback
import org.bibletranslationtools.writer.utils.FileUtilities
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.jetbrains.compose.resources.getString
import java.io.File

class CloneRepository(
    private val directoryProvider: DirectoryProvider,
    private val transportCallback: TransportCallback
) {

    companion object {
        private const val TAG = "CloneRepository"
    }

    suspend fun execute(
        cloneUrl: String,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): Result {
        onProgress(-1f, getString(Res.string.downloading))

        var tempDir: File? = directoryProvider.createTempDir()
        var status = Status.UNKNOWN

        try {
            // prepare destination
            val cloneCommand = Git.cloneRepository()
                .setTransportConfigCallback(transportCallback)
                .setURI(cloneUrl)
                .setDirectory(tempDir)
            try {
                val cloneResult = cloneCommand.call()
                cloneResult.repository.close()
                status = Status.SUCCESS
            } catch (e: TransportException) {
                Logger.e(TAG, e.message ?: "Error", e)
                val cause = e.cause
                if (cause != null) {
                    val subException = cause.cause
                    when {
                        subException != null -> {
                            val detail = subException.message
                            if ("Auth fail" == detail) {
                                status = Status.AUTH_FAILURE
                            }
                        }
                        cause.message!!.contains("not permitted") -> {
                            status = Status.AUTH_FAILURE
                        }
                        cause is NoRemoteRepositoryException -> {
                            status = Status.NO_REMOTE_REPO
                        }
                        else -> status = Status.NO_REMOTE_REPO
                    }
                }
            } catch (e: OutOfMemoryError) {
                Logger.e(TAG, e.message ?: "Error", e)
                status = Status.OUT_OF_MEMORY
            } catch (e: Throwable) {
                Logger.e(TAG, e.message ?: "Error", e)
            }
        } catch (e: Exception) {
            Logger.e(
                TAG,
                "Failed to clone the repository $cloneUrl", e
            )
        } finally {
            if (status != Status.SUCCESS) {
                FileUtilities.deleteQuietly(tempDir)
                tempDir = null
            }
        }

        return Result(status, cloneUrl, tempDir)
    }

    data class Result(val status: Status, val cloneUrl: String, val cloneDir: File?)

    enum class Status {
        NO_REMOTE_REPO,
        UNKNOWN,
        AUTH_FAILURE,
        OUT_OF_MEMORY,
        SUCCESS
    }
}