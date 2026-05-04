package org.bibletranslationtools.writer

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.app_name
import org.bibletranslationtools.writer.utils.getStringBlocking
import java.io.File

class DesktopDirectoryProvider : DirectoryProvider {
    private val os = System.getProperty("os.name").lowercase()
    private val home = System.getProperty("user.home")
    private val appName = getStringBlocking(Res.string.app_name)

    override val internalAppDir: File
        get() {
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

    override val externalAppDir: File
        get() {
            val dataFolder = File(home, appName)
            if (!dataFolder.exists()) dataFolder.mkdirs()
            return dataFolder
        }

    override val cacheDir: File
        get() = run {
            val file = File(internalAppDir, "temp")
            file.mkdirs()
            file
        }
}