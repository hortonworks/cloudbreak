package com.sequenceiq.cloudbreak.service.stack.flow;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

import net.schmizz.sshj.SSHClient;

@Component
public class SshCheckerTask extends StackBasedStatusCheckerTask<SshCheckerTaskContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshCheckerTask.class);

    @Override
    public boolean checkStatus(SshCheckerTaskContext sshCheckerTaskContext) {
        final SSHClient ssh = new SSHClient();
        boolean ret = false;
        try {
            ssh.addHostKeyVerifier(sshCheckerTaskContext.getHostKeyVerifier());
            ssh.connect(sshCheckerTaskContext.getPublicIp(), TlsSetupService.SSH_PORT);
            if (sshCheckerTaskContext.getStack().getCredential().passwordAuthenticationRequired()) {
                ssh.authPassword(sshCheckerTaskContext.getUser(), sshCheckerTaskContext.getStack().getCredential().getLoginPassword());
            } else {
                ssh.authPublickey(sshCheckerTaskContext.getUser(), sshCheckerTaskContext.getSshPrivateFileLocation());
            }
            ret = true;
        } catch (Exception e) {
            LOGGER.info("Failed to connect ssh: {}", e.getMessage());
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                LOGGER.info("Failed to disconnect from ssh: {}", e.getMessage());
            }
        }
        return ret;
    }

    @Override
    public void handleTimeout(SshCheckerTaskContext sshCheckerTaskContext) {
        throw new CloudbreakServiceException("Operation timed out. Could not reach ssh connection in time");
    }

    @Override
    public String successMessage(SshCheckerTaskContext sshCheckerTaskContext) {
        return "Ssh is up and running tls setup start.";
    }
}
