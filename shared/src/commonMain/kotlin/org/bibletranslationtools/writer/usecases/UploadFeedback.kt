package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.gogs_user_agent
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BuildInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.getHttpReporter
import org.bibletranslationtools.writer.utils.FileUtilities
import org.jetbrains.compose.resources.getString
import java.io.IOException

class UploadFeedback(
    private val directoryProvider: DirectoryProvider
) {
    companion object {
        private const val TAG = "UploadFeedback"
    }

    /**
     * Returns true if the upload was successful
     */
    suspend fun execute(notes: String, userEmail: String): Boolean {
        var uploaded: Boolean
        val logFile = directoryProvider.logFile

        val reporter = getHttpReporter(
            url = Preference.HELPDESK_WEBHOOK_URL + BuildInfo.HELPDESK_TOKEN,
            userEmail = userEmail.ifEmpty { Preference.DEFAULT_HELPDESK_EMAIL },
            userAgent = getString(Res.string.gogs_user_agent)
        )
        uploaded = reporter.reportBug(notes, logFile)

        if (uploaded) {
            try {
                FileUtilities.writeStringToFile(logFile, "")
            } catch (_: IOException) {
                Logger.i(TAG, "Failed to reset log file")
            }
            Logger.i(TAG, "Submitted bug report")
        } else {
            val error = reporter.getLastResponse()
            Logger.w(TAG, "Failed to upload bug report. Code: ${error?.code}, message: ${error?.message}")
        }

        return uploaded
    }
}