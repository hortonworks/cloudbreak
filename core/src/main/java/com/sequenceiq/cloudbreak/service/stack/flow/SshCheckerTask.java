package com.sequenceiq.cloudbreak.service.stack.flow;

import java.io.IOException;
import java.security.KeyPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.KeyStoreUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyPairWrapper;

@Component
public class SshCheckerTask extends StackBasedStatusCheckerTask<SshCheckerTaskContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshCheckerTask.class);

    @Override
    public boolean checkStatus(SshCheckerTaskContext sshCheckerTaskContext) {
        final SSHClient ssh = new SSHClient();
        boolean ret = false;
        try {
            ssh.addHostKeyVerifier(sshCheckerTaskContext.getHostKeyVerifier());
            String user = sshCheckerTaskContext.getUser();
            ssh.connect(sshCheckerTaskContext.getPublicIp(), sshCheckerTaskContext.getSshPort());
            if (sshCheckerTaskContext.getStack().passwordAuthenticationRequired()) {
                LOGGER.info("Connecting with ssh to: {}, user: {} with password", sshCheckerTaskContext.getPublicIp(), user);
                ssh.authPassword(user, sshCheckerTaskContext.getStack().getLoginPassword());
            } else {
                LOGGER.info("Connecting with ssh to: {}, user: {}", sshCheckerTaskContext.getPublicIp(), user);
                KeyPair keyPair = KeyStoreUtil.createKeyPair(sshCheckerTaskContext.getSshPrivateKey());
                ssh.authPublickey(user, new KeyPairWrapper(keyPair));
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
