package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudProperties;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Component
public class SshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshService.class);

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public String getRecipeExecutionOutput(String publicIp, String sshUser, String filePath) {
        SSHClient sshClient = createSshClient(publicIp, sshUser);
        return executeCommand(sshClient, "cat " + filePath);
    }

    private SSHClient createSshClient(String host, String sshUser) {
        try {
            SSHClient client = new SSHClient();
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect(host, 22);
            client.setConnectTimeout(commonCloudProperties.getSshTimeout());
            client.authPublickey(sshUser, commonCloudProperties.getSshPrivateKeyPath());
            return client;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String executeCommand(SSHClient ssh, String command) {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh);
                    Command cmd = session.exec(command);
                    OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
            LOGGER.info("The following SSH command is going to be executed on host {}: {}", ssh.getConnection().getTransport().getRemoteHost(), command);
            cmd.join(10L, TimeUnit.SECONDS);
            return os.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Session startSshSession(SSHClient ssh) throws IOException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }

}