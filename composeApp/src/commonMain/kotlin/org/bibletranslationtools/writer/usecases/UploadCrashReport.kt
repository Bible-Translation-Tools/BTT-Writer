package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_user_agent
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BuildInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.getHttpReporter
import org.jetbrains.compose.resources.getString
import java.io.IOException

class UploadCrashReport(
    private val directoryProvider: DirectoryProvider
) {
    companion object {
        private const val TAG = "UploadCrashReport"
    }

    suspend fun execute(notes: String, email: String): Boolean {
        var uploaded = false

        val logFile = directoryProvider.logFile

        val reporter = getHttpReporter(
            url = Preference.HELPDESK_WEBHOOK_URL + BuildInfo.HELPDESK_TOKEN,
            userEmail = email.ifEmpty { Preference.DEFAULT_HELPDESK_EMAIL },
            userAgent = getString(Res.string.gogs_user_agent)
        )
        val stackTraces = Logger.listStacktraces()
        if (stackTraces.isNotEmpty()) {
            try {
                // upload most recent stacktrace
                uploaded = reporter.reportCrash(notes, stackTraces[0], logFile)
            } catch (e: IOException) {
                Logger.w(TAG, "Failed to report crash", e)
            }

            if (!uploaded) {
                Logger.e(
                    this::class.java.simpleName,
                    "Failed to upload crash report."
                )
            } else { // success
                // empty the log
                Logger.flush()
            }
        }

        return uploaded
    }
}