package com.sequenceiq.it.cloudbreak

import java.io.IOException

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.it.ssh.MockSshServer

class StopSshServerTest : AbstractCloudbreakIntegrationTest() {

    @Inject
    private val mockSshServer: MockSshServer? = null

    @Test
    @Parameters("sshPort")
    fun stopSshServer(@Optional("22") sshPort: Int?) {
        try {
            mockSshServer!!.stop(sshPort!!)
            LOGGER.info("ssh server stopped")
        } catch (e: IOException) {
            throw RuntimeException("can't stop SSH server", e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StopSshServerTest::class.java)
    }
}
