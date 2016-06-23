package com.sequenceiq.it.ssh

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.HashMap

import javax.inject.Inject

import org.apache.commons.io.IOUtils
import org.apache.sshd.common.keyprovider.AbstractFileKeyPairProvider
import org.apache.sshd.common.util.SecurityUtils
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.scp.ScpCommandFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service

@Service
class MockSshServer {

    private val sshServerMap = HashMap<Int, SshServer>()

    @Inject
    private val resourceLoader: ResourceLoader? = null

    @Throws(IOException::class)
    fun start(port: Int) {
        if (sshServerMap[port] == null) {
            val sshServer = SshServer.setUpDefaultServer()
            val fileKeyPairProvider = SecurityUtils.createFileKeyPairProvider()
            fileKeyPairProvider.setFiles(setOf<File>(hostkey))
            fileKeyPairProvider.setPasswordFinder { resourceKey -> "cloudbreak" }
            sshServer.keyPairProvider = fileKeyPairProvider
            sshServer.setPublickeyAuthenticator { username, key, session -> true }
            setCommandFactory(sshServer)
            sshServer.fileSystemFactory = MockFileSystemFactory()
            sshServer.port = port
            sshServer.start()
            sshServerMap.put(port, sshServer)
        }
    }

    private fun setCommandFactory(sshServer: SshServer) {
        val scpCommandFactory = ScpCommandFactory()
        scpCommandFactory.delegateCommandFactory = MockCommandFactory()
        sshServer.commandFactory = scpCommandFactory
    }

    private val hostkey: File
        get() {
            try {
                val sshPemInputStream = resourceLoader!!.getResource("classpath:ssh.pem").inputStream
                val tempFile = File("ssh.pem")
                try {
                    FileOutputStream(tempFile).use { outputStream -> IOUtils.copy(sshPemInputStream, outputStream) }
                } catch (e: IOException) {
                    LOGGER.error("can't write ssh.pem", e)
                }

                return tempFile
            } catch (e: IOException) {
                throw RuntimeException("hostkey not found", e)
            }

        }

    @Throws(IOException::class)
    fun stop(port: Int) {
        val sshServer = sshServerMap[port]
        if (sshServer != null) {
            sshServer.stop()
            sshServerMap.remove(port)
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MockSshServer::class.java)
    }
}
