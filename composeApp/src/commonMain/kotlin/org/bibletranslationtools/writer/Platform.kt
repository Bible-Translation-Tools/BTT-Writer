package org.bibletranslationtools.writer

import java.io.File

interface Platform {
    val deviceId: String
    val appExternalDir: File
    val appInternalDir: File
}

expect fun getPlatform(): Platform