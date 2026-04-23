package org.bibletranslationtools.writer

class Greeting(
    val directoryProvider: DirectoryProvider
) {
    private val platform = getPlatform()

    fun greet(): String {
        println(directoryProvider.externalAppDir)
        return "Hello, ${platform.deviceId}!"
    }
}