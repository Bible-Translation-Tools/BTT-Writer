package org.bibletranslationtools.writer

class Greeting(
    private val directoryProvider: DirectoryProvider,
    private val platform: Platform
) {
    fun greet(): String {
        println(directoryProvider.externalAppDir)
        return "Hello, ${platform.udid}!"
    }
}