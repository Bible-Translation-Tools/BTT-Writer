package org.bibletranslationtools.writer

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.keys_dir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.utils.FileUtilities
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

interface DirectoryProvider {

    companion object {
        const val TAG = "DirectoryProvider"
    }

    /**
     * Returns the path to the internal files directory accessible by the app only.
     * This directory is not accessible by other applications and file managers.
     * It's good for storing private data, such as ssh keys.
     * Files saved in this directory will be removed when the application is uninstalled
     */
    val internalAppDir: File

    /**
     * Returns the path to the external files directory accessible by the app only.
     * This directory can be accessed by file managers.
     * It's good for storing user-created data, such as translations and backups.
     * Files saved in this directory will be removed when the application is uninstalled
     */
    val externalAppDir: File

    /**
     * Returns the absolute path to the application specific cache directory on the filesystem.
     */
    val cacheDir: File

    val translationsDir: File
        get() = File(externalAppDir, "translations")

    /**
     * Returns the local translations cache directory.
     * This is where import and export operations can expand files.
     */
    val translationsCacheDir: File
        get() = File(translationsDir, "cache")

    val databaseDir: File
        get() = run {
            val root = externalAppDir.parentFile
            val databaseDir = File(root, "databases")
            if (!databaseDir.exists()) {
                databaseDir.mkdirs()
            }
            databaseDir
        }

    /**
     * The index (database) file
     */
    val databaseFile: File
        get() = File(databaseDir, "index.sqlite")

    /**
     * The directory where all source resource containers will be stored
     */
    val containersDir: File
        get() = File(externalAppDir, "resource_containers")

    /**
     * The directory where all backup files will be stored
     */
    val backupsDir: File
        get() = File(externalAppDir, "backups")

    /**
     * Returns the sharing directory
     * @return
     */
    val sharingDir: File
        get() = run {
            val file = File(cacheDir, "sharing")
            file.mkdirs()
            file
        }

    /**
     * Returns the log file
     */
    val logFile: File
        get() = File(externalAppDir, "log.txt")

    /**
     * Returns the directory in which the ssh keys are stored
     */
    val sshKeysDir: File
        get() = run {
            val dir = File(
                internalAppDir,
                runBlocking { getString(Res.string.keys_dir) }
            )
            if (!dir.exists()) {
                dir.mkdir()
            }
            dir
        }

    /**
     * Returns the public key file
     */
    val publicKey: File
        get() = File(sshKeysDir, "id_rsa.pub")

    /**
     * Returns the private key file
     */
    val privateKey: File
        get() = File(sshKeysDir, "id_rsa")

    /**
     * Checks if the ssh keys have already been generated
     * @return Boolean
     */
    fun hasSSHKeys(): Boolean {
        return privateKey.exists() && publicKey.exists()
    }

    /**
     * Generates a new RSA key pair for use with ssh
     */
    suspend fun generateSSHKeys()

    /**
     * Moves an asset into the cache directory and returns a file reference to it
     * @param path
     * @return File
     */
    suspend fun getAssetAsFile(path: String): File {
        return withContext(Dispatchers.IO) {
            val cacheFile = File(cacheDir, "assets/$path")
            if (!cacheFile.exists()) {
                cacheFile.parentFile?.mkdirs()
                val bytes = Res.readBytes("files/$path")
                cacheFile.writeBytes(bytes)
            }
            cacheFile
        }
    }

    /**
     * Deploys the default index and resource containers.
     * @throws Exception
     */
    suspend fun deployDefaultLibrary() {
        Logger.i(TAG, "Deploying the default library to " + containersDir.parentFile)

        withContext(Dispatchers.IO) {
            // delete old database first
            FileUtilities.deleteQuietly(databaseFile)

            val bytes = Res.readBytes("files/index.sqlite")
            databaseFile.outputStream().use { out ->
                out.write(bytes)
            }
        }
    }

    /**
     * Nuke all the things!
     * ... or just the source content
     */
    suspend fun deleteLibrary() {
        withContext(Dispatchers.IO) {
            FileUtilities.deleteQuietly(databaseFile)
            FileUtilities.deleteQuietly(containersDir)
        }
    }

    /**
     * Creates a temporary directory.
     */
    suspend fun createTempDir(name: String? = null): File {
        return withContext(Dispatchers.IO) {
            val tempName = name ?: System.currentTimeMillis().toString()
            val tempDir = File(cacheDir, tempName)
            tempDir.mkdirs()
            tempDir
        }
    }

    /**
     * Creates a temporary file.
     * @param prefix Temp file prefix
     * @param suffix Temp file suffix
     * @param dir Directory to create temp file in
     */
    suspend fun createTempFile(prefix: String, suffix: String? = null, dir: File? = null): File {
        return withContext(Dispatchers.IO) {
            File.createTempFile(prefix, suffix, dir ?: cacheDir)
        }
    }

    /**
     * Writes a string to a file
     * @param file
     * @param contents
     * @throws IOException
     */
    @Throws(IOException::class)
    suspend fun writeStringToFile(file: File, contents: String) {
        withContext(Dispatchers.IO) {
            FileOutputStream(file).use { fos ->
                fos.write(contents.toByteArray())
            }
        }
    }

    /**
     * Clear the cache directory
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.forEach {
                FileUtilities.deleteQuietly(it)
            }
        }
    }
}