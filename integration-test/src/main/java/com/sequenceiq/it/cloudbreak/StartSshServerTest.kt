package com.sequenceiq.it.cloudbreak

import java.io.IOException

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.it.ssh.MockSshServer

class StartSshServerTest : AbstractCloudbreakIntegrationTest() {

    @Inject
    private val mockSshServer: MockSshServer? = null

    @Test
    @Parameters("sshPort")
    fun startSshServer(@Optional("22") sshPort: Int?) {
        try {
            mockSshServer!!.start(sshPort!!)
            LOGGER.info("ssh server started on port: " + sshPort)
        } catch (e: IOException) {
            throw RuntimeException("ssh server can't start", e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StartSshServerTest::class.java)
    }
}
