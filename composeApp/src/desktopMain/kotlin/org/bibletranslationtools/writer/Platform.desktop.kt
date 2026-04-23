package org.bibletranslationtools.writer

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.app_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import java.io.File

class DesktopPlatform : Platform {
    override val deviceId: String
        get() = (System.getProperty("os.name") + "_" + System.getProperty("os.arch"))
            .lowercase().replace(" ", "_")

    override val appExternalDir: File
        get() {
            val appName = runBlocking { getString(Res.string.app_name) }
            val dataFolder = File(System.getProperty("user.home"), appName)
            if (!dataFolder.exists()) dataFolder.mkdirs()
            return dataFolder
        }

    override val appInternalDir: File
        get() {
            val appName = runBlocking { getString(Res.string.app_name) }
            val os = System.getProperty("os.name").lowercase()
            val home = System.getProperty("user.home")

            val baseDir = when {
                // Windows: C:\Users\Name\AppData\Roaming
                os.contains("win") -> {
                    System.getenv("APPDATA") ?: "$home\\AppData\\Roaming"
                }

                // macOS: /Users/Name/Library/Application Support
                os.contains("mac") -> "$home/Library/Application Support"

                // Linux: /home/name/.config (respects XDG spec)
                else -> System.getenv("XDG_CONFIG_HOME") ?: "$home/.config"
            }

            val configFolder = File(baseDir, appName)
            if (!configFolder.exists()) configFolder.mkdirs()

            return configFolder
        }

}

actual fun getPlatform(): Platform = DesktopPlatform()