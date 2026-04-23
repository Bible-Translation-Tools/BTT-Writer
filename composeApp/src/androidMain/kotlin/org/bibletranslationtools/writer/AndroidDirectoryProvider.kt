package org.bibletranslationtools.writer

import android.content.Context
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidDirectoryProvider(
    private val context: Context
) : DirectoryProvider {

    override val internalAppDir: File
        get() = getPlatform().appInternalDir

    override val externalAppDir: File
        get() = getPlatform().appExternalDir

    override val cacheDir: File
        get() = context.cacheDir

    override suspend fun generateSSHKeys() {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            val type = KeyPair.RSA

            try {
                val keyPair = KeyPair.genKeyPair(jsch, type)
                File(privateKey.absolutePath).createNewFile()
                keyPair.writePrivateKey(privateKey.absolutePath)
                File(publicKey.absolutePath).createNewFile()
                keyPair.writePublicKey(publicKey.absolutePath, getPlatform().deviceId)
                keyPair.dispose()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}