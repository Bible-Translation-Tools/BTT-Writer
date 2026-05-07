package org.bibletranslationtools.writer.usecases

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BuildInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.getGithubReporter
import java.io.IOException

class UploadCrashReport(
    private val directoryProvider: DirectoryProvider,
    private val preference: Preference
) {
    companion object {
        val TAG = UploadCrashReport::javaClass.name
    }

    suspend fun execute(message: String): Boolean {
        var uploaded = false

        val logFile = directoryProvider.logFile
        val githubTokenIdentifier = BuildInfo.OAUTH_TOKEN
        val githubUrl = preference.getGithubBugReportRepo()

        // TRICKY: make sure the github_oauth2 token has been set
        if (githubTokenIdentifier.isNotEmpty()) {
            val reporter = getGithubReporter(
                repoUrl = githubUrl,
                oAuthToken = githubTokenIdentifier
            )
            val stackTraces = Logger.listStacktraces()
            if (stackTraces.isNotEmpty()) {
                try {
                    // upload most recent stacktrace
                    uploaded = reporter.reportCrash(message, stackTraces[0], logFile)
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
        }

        return uploaded
    }
}