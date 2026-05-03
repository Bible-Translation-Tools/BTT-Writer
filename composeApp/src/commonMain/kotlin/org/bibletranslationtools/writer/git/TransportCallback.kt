package org.bibletranslationtools.writer.git

import org.bibletranslationtools.writer.DirectoryProvider
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport

class TransportCallback(directoryProvider: DirectoryProvider) : TransportConfigCallback {
    private val ssh = SshSessionFactory.create(directoryProvider)

    override fun configure(tn: Transport) {
        if (tn is SshTransport) {
            tn.sshSessionFactory = ssh
        }
    }
}
