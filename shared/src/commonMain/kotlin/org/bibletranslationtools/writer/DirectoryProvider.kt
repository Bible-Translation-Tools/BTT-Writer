package org.bibletranslationtools.writer

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.keys_dir
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.git.SSHConfigurator
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import org.bibletranslationtools.writer.utils.getStringBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

interface DirectoryProvider {

    companion object {
        private const val TAG = "DirectoryProvider"
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
        get() = File(externalAppDir, "translations").apply {
            mkdirs()
        }

    val databaseDir: File
        get() = File(externalAppDir, "database").apply {
            mkdirs()
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
        get() = File(externalAppDir, "resource_containers").apply {
            mkdirs()
        }

    /**
     * The directory where all backup files will be stored
     */
    val backupsDir: File
        get() = File(externalAppDir, "backups").apply {
            mkdirs()
        }

    /**
     * The directory where user-imported custom fonts are stored. Scanned by the
     * system font providers so imported fonts appear in the font picker.
     */
    val fontsDir: File
        get() = File(externalAppDir, "fonts").apply {
            mkdirs()
        }

    /**
     * Returns the sharing directory
     * @return
     */
    val sharingDir: File
        get() = File(cacheDir, "sharing").apply {
            mkdirs()
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
        get() = File(
            internalAppDir,
            getStringBlocking(Res.string.keys_dir)
        ).apply {
            mkdirs()
        }

    /**
     * Returns the public key file
     */
    val publicKey: File
        get() = File(sshKeysDir, "id_ed25519.pub")

    /**
     * Returns the private key file
     */
    val privateKey: File
        get() = File(sshKeysDir, "id_ed25519")

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
    suspend fun generateSSHKeys(udid: String) {
        try {
            val (privateStr, publicStr) = SSHConfigurator.generateKeys(udid)
            privateKey.writeText(privateStr)
            publicKey.writeText(publicStr)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to generate SSH keys", e)
        }
    }

    /**
     * Opens an asset as a streaming InputStream. Override on platforms with native asset access
     * to avoid loading the entire file into memory via Res.readBytes.
     */
    suspend fun openAssetStream(path: String): InputStream {
        return Res.readBytes(path).inputStream()
    }

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
                openAssetStream(path).use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
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

            openAssetStream("files/index.sqlite").use { input ->
                databaseFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Delete old journal to avoid corrupt database errors
            val shmFile = File(databaseFile.absolutePath + "-shm")
            if (shmFile.exists()) { FileUtilities.deleteQuietly(shmFile) }
            val walFile = File(databaseFile.absolutePath + "-wal")
            if (walFile.exists()) { FileUtilities.deleteQuietly(walFile) }
            val journalFile = File(databaseFile.absolutePath + "-journal")
            if (journalFile.exists()) { FileUtilities.deleteQuietly(journalFile) }

            // extract resource containers
            containersDir.mkdirs()
            getAssetAsFile("files/containers.zip").inputStream().use {
                Zip.unzipFromStream(it, containersDir)
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
            val tempName = name ?: UUID.randomUUID().toString()
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
     * Copies a user-picked file into [dest] dir and returns the stored file.
     */
    suspend fun copyFile(file: PlatformFile, dest: File): File {
        if (!dest.isDirectory) {
            throw IllegalArgumentException("Dest should be a directory.")
        }

        return withContext(Dispatchers.IO) {
            val target = File(dest, file.name)
            file.inputStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            target
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

fun PlatformFile.inputStream(): InputStream {
    return source().buffered().asInputStream()
}

fun PlatformFile.outputStream(): OutputStream {
    return sink().buffered().asOutputStream()
}