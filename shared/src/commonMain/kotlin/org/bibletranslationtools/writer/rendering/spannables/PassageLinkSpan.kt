package org.bibletranslationtools.writer.rendering.spannables
import org.bibletranslationtools.logger.Logger
import java.util.regex.Pattern

class PassageLinkSpan(
    title: String,
    address: String
) : Span(title, address) {

    private var _title: String = title
    fun getTitle(): String = _title

    var languageId: String? = null
        private set

    var projectId: String? = null
        private set

    var chapterId: String = ""
        private set

    var frameId: String = ""
        private set

    companion object {
        private const val TAG = "PassageLinkSpan"
        // e.g. [[:en:bible:notes:gen:01:03|1:5]]
        val PATTERN: Pattern = Pattern.compile("\\[\\[:(((?!]]).)*)\\|(((?!]]).)*)]]")
    }

    init {
        explodeAddress(address)
    }

    /**
     * Changes the title of the passage link
     * @param title the new title
     */
    fun setTitle(title: String) {
        this._title = title
        humanReadable = title
    }

    /**
     * Breaks the address apart into its components
     * @param address the link address to explode
     */
    private fun explodeAddress(address: String) {
        val parts = address.split(":")
        if (parts.size == 6 && parts[1] == "bible") {
            // example: en:bible:notes:gen:03:04
            languageId = parts[0]
            projectId = parts[3]
            chapterId = parts[4]
            frameId = parts[5]
        } else if (parts.size == 5 && parts[3] == "frames") {
            // example: en:obs:notes:frames:01-11
            val chapterFrame = parts[4].split("-")
            if (chapterFrame.size == 2) {
                languageId = parts[0]
                projectId = parts[1]
                chapterId = chapterFrame[0]
                frameId = chapterFrame[1]
            }
        } else {
            Logger.w(TAG, "invalid passage link address $address")
        }
    }
}
