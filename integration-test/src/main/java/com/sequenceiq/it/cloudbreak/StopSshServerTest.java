package com.sequenceiq.it.cloudbreak;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.ssh.MockSshServer;

public class StopSshServerTest extends AbstractCloudbreakIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopSshServerTest.class);

    @Inject
    private MockSshServer mockSshServer;

    @Test
    @Parameters({ "sshPort" })
    public void stopSshServer(@Optional("22") Integer sshPort) {
        try {
            mockSshServer.stop(sshPort);
            LOGGER.info("ssh server stopped");
        } catch (IOException e) {
            throw new RuntimeException("can't stop SSH server", e);
        }
    }
}
