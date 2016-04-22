package com.sequenceiq.it.cloudbreak;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.ssh.MockSshServer;

public class StartSshServerTest extends AbstractCloudbreakIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartSshServerTest.class);

    @Inject
    private MockSshServer mockSshServer;

    @Test
    @Parameters({ "sshPort" })
    public void startSshServer(@Optional("sshPort") Integer sshPort) {
        mockSshServer.setPort(sshPort);
        try {
            mockSshServer.start();
            LOGGER.info("ssh server started on port: " + sshPort);
        } catch (IOException e) {
            throw new RuntimeException("ssh server can't start", e);
        }
    }
}
