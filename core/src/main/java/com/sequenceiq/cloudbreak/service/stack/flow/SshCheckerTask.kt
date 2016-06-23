package com.sequenceiq.cloudbreak.service.stack.flow

import java.io.IOException

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask

import net.schmizz.sshj.SSHClient

@Component
class SshCheckerTask : StackBasedStatusCheckerTask<SshCheckerTaskContext>() {

    override fun checkStatus(sshCheckerTaskContext: SshCheckerTaskContext): Boolean {
        val ssh = SSHClient()
        var ret = false
        try {
            ssh.addHostKeyVerifier(sshCheckerTaskContext.hostKeyVerifier)
            val user = sshCheckerTaskContext.user
            ssh.connect(sshCheckerTaskContext.publicIp, sshCheckerTaskContext.sshPort)
            if (sshCheckerTaskContext.stack.credential.passwordAuthenticationRequired()) {
                LOGGER.info("Connecting with ssh to: {}, user: {} with password", sshCheckerTaskContext.publicIp, user)
                ssh.authPassword(user, sshCheckerTaskContext.stack.credential.loginPassword)
            } else {
                LOGGER.info("Connecting with ssh to: {}, user: {}, privatekey: {}", sshCheckerTaskContext.publicIp, user, sshCheckerTaskContext.sshPrivateFileLocation)
                ssh.authPublickey(user, sshCheckerTaskContext.sshPrivateFileLocation)
            }
            ret = true
        } catch (e: Exception) {
            LOGGER.info("Failed to connect ssh: {}", e.message)
        } finally {
            try {
                ssh.disconnect()
            } catch (e: IOException) {
                LOGGER.info("Failed to disconnect from ssh: {}", e.message)
            }

        }
        return ret
    }

    override fun handleTimeout(sshCheckerTaskContext: SshCheckerTaskContext) {
        throw CloudbreakServiceException("Operation timed out. Could not reach ssh connection in time")
    }

    override fun successMessage(sshCheckerTaskContext: SshCheckerTaskContext): String {
        return "Ssh is up and running tls setup start."
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SshCheckerTask::class.java)
    }
}
