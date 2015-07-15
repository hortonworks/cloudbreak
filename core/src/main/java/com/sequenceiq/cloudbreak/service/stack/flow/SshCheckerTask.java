package com.sequenceiq.cloudbreak.service.stack.flow;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

import net.schmizz.sshj.SSHClient;

@Component
public class SshCheckerTask extends StackBasedStatusCheckerTask<SshCheckerTaskContext> {

    @Override
    public boolean checkStatus(SshCheckerTaskContext sshCheckerTaskContext) {
        final SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier(sshCheckerTaskContext.getHostKeyVerifier());
            ssh.connect(sshCheckerTaskContext.getPublicIp(), TlsSetupService.SSH_PORT);
            ssh.authPublickey(sshCheckerTaskContext.getUser(), sshCheckerTaskContext.getSshPrivateFileLocation());
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                return false;
            }
        }
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
