package org.bibletranslationtools.writer.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.path
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.displayName
import java.io.Closeable
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * This class provides some utility methods for handling files
 */
object FileUtilities {

    /**
     * Filter applied during recursive directory copy.
     * Return `true` to include the file, `false` to skip it.
     */
    fun interface PlatformFileFilter {
        suspend fun accept(file: PlatformFile): Boolean
    }

    /**
     * Converts an input stream into a string
     * @param stream
     * @return
     * @throws Exception
     */
    @Throws(IOException::class)
    fun readStreamToString(stream: InputStream): String {
        return stream.bufferedReader().use { it.readText() }
    }

    /**
     * Returns the contents of a file as a string
     * @param file
     * @return
     * @throws Exception
     */
    @Throws(IOException::class)
    fun readFileToString(file: File): String {
        FileInputStream(file).use { fis ->
            return readStreamToString(fis)
        }
    }

    /**
     * Writes a string to a file
     * @param file
     * @param contents
     * @throws IOException
     */
    @Throws(IOException::class)
    fun writeStringToFile(file: File, contents: String) {
        FileOutputStream(file).use { fos ->
            fos.write(contents.toByteArray())
        }
    }

    @Throws(IOException::class)
    fun copyInputStreamToFile(source: InputStream, destination: File) {
        source.use { input ->
            openOutputStream(destination).use { output ->
                copy(input, output)
            }
        }
    }

    @Throws(IOException::class)
    fun openOutputStream(file: File, append: Boolean = false): FileOutputStream {
        if (file.exists()) {
            if (file.isDirectory) {
                throw IOException("File \'$file\' exists but is a directory")
            }
            if (!file.canWrite()) {
                throw IOException("File \'$file\' cannot be written to")
            }
        } else {
            val parent = file.parentFile
            if (parent != null && !parent.mkdirs() && !parent.isDirectory) {
                throw IOException("Directory \'$parent\' could not be created")
            }
        }

        return FileOutputStream(file, append)
    }

    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream): Int {
        val count = copyLarge(input, output)
        return if (count > 2147483647L) -1 else count.toInt()
    }

    /**
     * Returns the extension of the file.
     * If no delimiter is found or there is no extension the result is an empty string
     * @param path
     * @return
     */
    fun getExtension(path: String): String {
        val index = path.lastIndexOf(".")
        if (index == -1 || index == path.length - 1) {
            return ""
        }
        return path.substring(index + 1)
    }

    @Throws(IOException::class)
    fun copyLarge(
        input: InputStream,
        output: OutputStream,
        buffer: ByteArray? = ByteArray(4096)
    ): Long {
        var count = 0L
        var n1: Int

        while (-1 != (input.read(buffer).also { n1 = it })) {
            output.write(buffer, 0, n1)
            count += n1.toLong()
        }

        return count
    }

    /**
     * Recursively deletes a directory or just deletes the file
     * @param fileOrDirectory
     */
    fun deleteQuietly(fileOrDirectory: File?): Boolean {
        if (fileOrDirectory != null) {
            if (fileOrDirectory.isDirectory) {
                for (child in fileOrDirectory.listFiles()) {
                    if (!deleteQuietly(child)) {
                        return false
                    }
                }
            }
            if (fileOrDirectory.exists()) {
                try {
                    fileOrDirectory.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }
        }
        return true
    }

    /**
     * Attempts to move a file or directory. If moving fails it will try to copy instead.
     * @param sourceFile
     * @param destFile
     * @return
     */
    fun moveOrCopyQuietly(sourceFile: File, destFile: File): Boolean {
        if (sourceFile.exists()) {
            // first try to move
            if (!sourceFile.renameTo(destFile)) {
                // try to copy
                try {
                    if (sourceFile.isDirectory) {
                        copyDirectory(sourceFile, destFile, null)
                    } else {
                        copyFile(sourceFile, destFile)
                    }
                    return true
                } catch (e: IOException) {
                    Logger.e(FileUtilities::class.java.name, "Failed to copy the file", e)
                }
            } else {
                return true // successful move
            }
        }
        return false
    }

    /**
     * Deletes a file/directory by first moving it to a temporary location then deleting it.
     * This avoids an issue with FAT32 on some devices where you cannot create a file
     * with the same name right after deleting it
     * @param file
     */
    fun safeDelete(file: File?) {
        if (file != null && file.exists()) {
            val temp = File(file.parentFile, System.currentTimeMillis().toString() + ".trash")
            file.renameTo(temp)
            if (file.isDirectory) {
                moveOrCopyQuietly(file, File(temp, file.name))
            } else {
                moveOrCopyQuietly(file, temp)
            }
            deleteQuietly(file) // just in case the move failed
            deleteQuietly(temp)
        }
    }

    @Throws(IOException::class)
    fun copyDirectory(srcDir: File, destDir: File, filter: FileFilter?) {
        if (!srcDir.exists()) {
            throw FileNotFoundException("Source \'$srcDir\' does not exist")
        } else if (!srcDir.isDirectory) {
            throw IOException("Source \'$srcDir\' exists but is not a directory")
        } else if (srcDir.canonicalPath == destDir.canonicalPath) {
            throw IOException("Source \'$srcDir\' and destination \'$destDir\' are the same")
        } else {
            val exclusionList = arrayListOf<String>()
            if (destDir.canonicalPath.startsWith(srcDir.canonicalPath)) {
                val srcFiles = if (filter == null) srcDir.listFiles() else srcDir.listFiles(filter)
                if (srcFiles != null && srcFiles.isNotEmpty()) {
                    val len = srcFiles.size
                    for (i in 0 until len) {
                        val srcFile = srcFiles[i]
                        val copiedFile = File(destDir, srcFile.name)
                        exclusionList.add(copiedFile.canonicalPath)
                    }
                }
            }

            doCopyDirectory(srcDir, destDir, filter, exclusionList)
        }
    }

    @Throws(IOException::class)
    private fun doCopyDirectory(
        srcDir: File,
        destDir: File,
        filter: FileFilter?,
        exclusionList: List<String>?
    ) {
        val srcFiles = if (filter == null) srcDir.listFiles() else srcDir.listFiles(filter)
        if (srcFiles == null) {
            throw IOException("Failed to list contents of $srcDir")
        } else {
            if (destDir.exists()) {
                if (!destDir.isDirectory) {
                    throw IOException("Destination \'$destDir\' exists but is not a directory")
                }
            } else if (!destDir.mkdirs() && !destDir.isDirectory) {
                throw IOException("Destination \'$destDir\' directory cannot be created")
            }

            if (!destDir.canWrite()) {
                throw IOException("Destination \'$destDir\' cannot be written to")
            } else {
                val len = srcFiles.size
                for (i in 0 until len) {
                    val srcFile = srcFiles[i]
                    val dstFile = File(destDir, srcFile.name)
                    if (exclusionList == null || !exclusionList.contains(srcFile.canonicalPath)) {
                        if (srcFile.isDirectory) {
                            doCopyDirectory(srcFile, dstFile, filter, exclusionList)
                        } else {
                            doCopyFile(srcFile, dstFile)
                        }
                    }
                }

                // reserve date
                destDir.setLastModified(srcDir.lastModified())
            }
        }
    }

    /**
     * Recursively copies [srcDir] into [destDir], optionally applying [filter].
     *
     * - Throws if [srcDir] doesn't exist or isn't a directory.
     * - Throws if source and destination are the same path.
     * - When [destDir] is nested inside [srcDir], the entries that would be
     *   created in [destDir] are excluded from the copy to prevent infinite recursion.
     */
    @Throws(IOException::class)
    suspend fun copyDirectory(
        srcDir: PlatformFile,
        destDir: PlatformFile,
        filter: PlatformFileFilter? = null
    ) {
        if (!srcDir.exists()) {
            throw FileNotFoundException("Source '${srcDir.path}' does not exist")
        }
        if (!srcDir.isDirectory()) {
            throw IOException("Source '${srcDir.path}' exists but is not a directory")
        }

        val srcPath = srcDir.absolutePath()
        val destPath = destDir.absolutePath()
        if (srcPath == destPath) {
            throw IOException("Source '${srcDir.path}' and destination '${destDir.path}' are the same")
        }

        // If destination is nested inside source, build a list of paths inside the
        // destination that we must NOT copy back into themselves.
        val exclusionList = mutableListOf<String>()
        if (destPath.startsWith(srcPath)) {
            val srcFiles = listChildren(srcDir, filter)
            srcFiles.forEach { srcFile ->
                val copiedFile = destDir / srcFile.displayName
                exclusionList.add(copiedFile.absolutePath())
            }
        }

        doCopyDirectory(srcDir, destDir, filter, exclusionList)
    }

    @Throws(IOException::class)
    private suspend fun doCopyDirectory(
        srcDir: PlatformFile,
        destDir: PlatformFile,
        filter: PlatformFileFilter?,
        exclusionList: List<String>
    ) {
        val srcFiles = try {
            listChildren(srcDir, filter)
        } catch (e: Exception) {
            throw IOException("Failed to list contents of ${srcDir.path}", e)
        }

        if (destDir.exists()) {
            if (!destDir.isDirectory()) {
                throw IOException("Destination '${destDir.path}' exists but is not a directory")
            }
        } else {
            try {
                destDir.createDirectories()
            } catch (e: Exception) {
                throw IOException("Destination '${destDir.path}' directory cannot be created", e)
            }
        }

        for (srcFile in srcFiles) {
            val dstFile = destDir / srcFile.displayName
            if (srcFile.absolutePath() in exclusionList) continue

            if (srcFile.isDirectory()) {
                doCopyDirectory(srcFile, dstFile, null, exclusionList)
            } else {
                srcFile.copyTo(dstFile)
            }
        }
    }

    private suspend fun listChildren(
        dir: PlatformFile,
        filter: PlatformFileFilter?
    ): List<PlatformFile> {
        val all = dir.list()
        return if (filter == null) all else all.filter { filter.accept(it) }
    }

    /**
     * Copies a file or directory
     * @param srcFile
     * @param destFile
     */
    @Throws(IOException::class)
    fun copyFile(srcFile: File, destFile: File) {
        if (!srcFile.exists()) {
            throw FileNotFoundException("Source \'$srcFile\' does not exist")
        } else if (srcFile.isDirectory) {
            throw IOException("Source \'$srcFile\' exists but is a directory")
        } else if (srcFile.canonicalPath == destFile.canonicalPath) {
            throw IOException("Source \'$srcFile\' and destination \'$destFile\' are the same")
        } else {
            val parentFile = destFile.parentFile
            if (parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory) {
                throw IOException("Destination \'$parentFile\' directory cannot be created")
            } else if (destFile.exists() && !destFile.canWrite()) {
                throw IOException("Destination \'$destFile\' exists but is read-only")
            } else {
                doCopyFile(srcFile, destFile)
            }
        }
    }

    @Throws(IOException::class)
    private fun doCopyFile(srcFile: File, destFile: File) {
        if (destFile.exists() && destFile.isDirectory) {
            throw IOException("Destination \'$destFile\' exists but is a directory")
        } else {
            FileInputStream(srcFile).use { fis ->
                FileOutputStream(destFile).use { fos ->
                    val input = fis.channel
                    val output = fos.channel
                    val size = input.size()
                    var pos = 0L
                    var count = 0L

                    while (pos < size) {
                        count = if (size - pos > 31457280L) 31457280L else size - pos
                        pos += output.transferFrom(input, pos, count)
                    }
                }
            }

            if (srcFile.length() != destFile.length()) {
                throw IOException("Failed to copy full contents from \'$srcFile\' to \'$destFile\'")
            } else {
                // preserve date
                destFile.setLastModified(srcFile.lastModified())
            }
        }
    }

    /**
     * closes the closable without throwing an exception
     * @param closable
     */
    fun closeQuietly(closable: Closeable) {
        try {
            closable.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun forceMkdir(directory: File) {
        val message: String
        if (directory.exists()) {
            if (!directory.isDirectory) {
                message =
                    "File $directory exists and is not a directory. Unable to create directory."
                throw IOException(message)
            }
        } else if (!directory.mkdirs() && !directory.isDirectory) {
            message = "Unable to create directory $directory"
            throw IOException(message)
        }
    }
}
