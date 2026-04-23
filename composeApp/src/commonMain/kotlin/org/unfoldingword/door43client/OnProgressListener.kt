package org.unfoldingword.door43client

/**
 * A utility to get progress updates during long operations
 */
interface OnProgressListener {
    /**
     * 
     * @param tag used to identify what progress event is occurring
     * @param max the total number of items being processed
     * @param complete the number of items that have been successfully processed
     * @return cancels the operation if false is returned
     */
    fun onProgress(tag: String, max: Int, complete: Int): Boolean

    /**
     *
     * @param tag used to identify what progress event is occurring
     * @param value the current progress value
     */
    fun onProgress(tag: String, value: Float)
}
