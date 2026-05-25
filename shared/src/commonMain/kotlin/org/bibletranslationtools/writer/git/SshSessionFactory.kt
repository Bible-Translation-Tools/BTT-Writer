package org.bibletranslationtools.writer.git

import org.bibletranslationtools.writer.DirectoryProvider
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import java.io.File

object SshSessionFactory {

    fun create(directoryProvider: DirectoryProvider): SshdSessionFactory {
        val configurator = SSHConfigurator(directoryProvider)

        configurator.setHomeDir()
        configurator.setSecurityProvider()

        return SshdSessionFactoryBuilder()
            .setServerKeyDatabase { _, _ -> configurator.trustAllDatabase }
            .setConfigFile { _ -> configurator.configFile }
            .setHomeDirectory(directoryProvider.internalAppDir)
            .setSshDirectory(File(directoryProvider.internalAppDir, "ssh"))
            .build(null)
    }
}