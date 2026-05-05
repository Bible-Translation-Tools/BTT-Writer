package org.bibletranslationtools.writer.usecases

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BuildInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.getGithubReporter
import org.bibletranslationtools.writer.utils.FileUtilities
import java.io.IOException

class UploadFeedback(
    private val preference: Preference,
    private val directoryProvider: DirectoryProvider
) {
    /**
     * Returns true if the upload was successful
     */
    suspend fun execute(notes: String): Boolean {
        var uploaded = false
        val logFile = directoryProvider.logFile

        // TRICKY: make sure the github_oauth2 token has been set
        val githubTokenIdentifier = BuildInfo.OAUTH_TOKEN
        val githubUrl = preference.getGithubBugReportRepo()

        if (githubTokenIdentifier.isNotEmpty()) {
            val reporter = getGithubReporter(
                repoUrl = githubUrl,
                oAuthToken = githubTokenIdentifier
            )
            try {
                uploaded = reporter.reportBug(notes, logFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (!uploaded) {
                Logger.e(
                    this.javaClass.name,
                    "Failed to upload bug report."
                )
            } else { // success
                try {
                    FileUtilities.writeStringToFile(logFile, "")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                Logger.i(this.javaClass.name, "Submitted bug report")
            }
        } else {
            Logger.w(this.javaClass.name, "the github oauth2 token is missing")
        }

        return uploaded
    }
}