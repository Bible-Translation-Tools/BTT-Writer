package org.bibletranslationtools.writer.utils

object StringUtilities {

    /**
     * Pads a slug to 2 significant digits.
     * Examples:
     * '1'    -> '01'
     * '001'  -> '01'
     * '12'   -> '12'
     * '123'  -> '123'
     * '0123' -> '123'
     * Words are not padded:
     * 'a' -> 'a'
     * '0word' -> '0word'
     * And as a matter of consistency:
     * '0'  -> '00'
     * '00' -> '00'
     *
     * @param slug the slug to be normalized
     * @return the normalized slug
     */
    @Throws(Exception::class)
    fun normalizeSlug(slug: String): String {
        if (slug.isEmpty()) throw Exception("slug cannot be an empty string")
        if (!isInteger(slug)) return slug

        var normalizedSlug = slug.replace("^(0+)".toRegex(), "").trim()
        while (normalizedSlug.length < 2) {
            normalizedSlug = "0$normalizedSlug"
        }
        return normalizedSlug
    }

    /**
     * Checks if a string is an integer
     * @param s
     * @return
     */
    fun isInteger(s: String): Boolean {
        return s.toIntOrNull() != null
    }

    /**
     * Returns a string formatted as an integer (removes the leading 0's
     * Otherwise it returns the original value
     * @param value the string to format
     * @return the number formatted string
     */
    fun formatNumber(value: String): String {
        return value.toIntOrNull()?.toString() ?: value
    }

    /**
     * Splits a string by delimiter into two pieces
     * @param string the string to split
     * @param delimiter
     * @return
     */
    fun chunk(string: String, delimiter: String): Array<String> {
        if (string.isEmpty()) {
            return arrayOf("", "")
        }
        var pieces = string.split(delimiter.toRegex(), limit = 2).toTypedArray()
        if (pieces.size == 1) {
            pieces = arrayOf(string, "")
        }
        return pieces
    }
}