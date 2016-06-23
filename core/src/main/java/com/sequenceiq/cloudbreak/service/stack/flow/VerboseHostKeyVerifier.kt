package com.sequenceiq.cloudbreak.service.stack.flow

import java.security.PublicKey

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.HostKeyVerifier

class VerboseHostKeyVerifier(private val expectedFingerprints: Set<String>) : HostKeyVerifier {

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        if (expectedFingerprints.isEmpty()) {
            return true
        }
        val receivedFingerprint = SecurityUtils.getFingerprint(key)
        var matches = false
        for (expectedFingerprint in expectedFingerprints) {
            matches = receivedFingerprint == expectedFingerprint
            if (matches) {
                break
            }
        }
        if (matches) {
            LOGGER.info("HostKey has been successfully verified. hostname: {}, port: {}, fingerprint: {}", hostname, port, receivedFingerprint)
        } else {
            LOGGER.error("HostKey verification failed. hostname: {}, port: {}, expectedFingerprint: {}, receivedFingerprint: {}", hostname, port,
                    expectedFingerprints, receivedFingerprint)
        }
        return matches
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(VerboseHostKeyVerifier::class.java)
    }
}

