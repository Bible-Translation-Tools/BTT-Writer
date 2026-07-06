package org.bibletranslationtools.writer.utils

object RuntimeWrapper {

    val availableProcessors: Int
        get() = Runtime.getRuntime().availableProcessors()

    val maxMemory: Long
        get() = Runtime.getRuntime().maxMemory()

    fun exit(status: Int) {
        Runtime.getRuntime().exit(status)
    }
}