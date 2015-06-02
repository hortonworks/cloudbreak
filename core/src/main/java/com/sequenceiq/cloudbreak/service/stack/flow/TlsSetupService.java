package com.sequenceiq.cloudbreak.service.stack.flow;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.service.ProvisioningSetupService;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    public static final int SSH_PORT = 22;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    public void setupTls(CloudPlatform cloudPlatform, Stack stack, Map<String, Object> setupProperties) throws CloudbreakException {

        InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);

        LOGGER.info("SSH into gateway node to setup certificates on gateway.");
        final SSHClient ssh = new SSHClient();
        String sshFingerprint = connector.getSSHThumbprint(stack, gateway.getInstanceId());
        ssh.addHostKeyVerifier(sshFingerprint);
        try {
            ssh.connect(gateway.getPublicIp(), SSH_PORT);
            ssh.authPublickey(connector.getSSHUser(), (String) setupProperties.get(ProvisioningSetupService.SSH_PRIVATEKEY_LOCATION));
            // TODO: SCP cloudbreak TLS cert

            final Session tlsSetupSession = ssh.startSession();
            final Session.Command tlsSetupCmd = tlsSetupSession.exec(FileReaderUtils.readFileFromClasspath("init/tls-setup.sh"));

            tlsSetupCmd.join(5, TimeUnit.SECONDS);
            LOGGER.info("exit status: " + tlsSetupCmd.getExitStatus());
            tlsSetupSession.close();

            final Session changeSshKeySession = ssh.startSession();
            final Session.Command changeSshKeyCmd = changeSshKeySession.exec("echo '" + stack.getCredential().getPublicKey() + "' > ~/.ssh/authorized_keys");
            LOGGER.info(IOUtils.readFully(changeSshKeyCmd.getInputStream()).toString());
            changeSshKeyCmd.join(5, TimeUnit.SECONDS);
            LOGGER.info("Change SSH key command exit status: " + changeSshKeyCmd.getExitStatus());
            changeSshKeySession.close();

            ssh.disconnect();
        } catch (IOException e) {
            throw new CloudbreakException("Failed to setup TLS through temporary SSH.");
        }
        connector.cleanupTemporarySSH(stack, gateway.getInstanceId());
    }
}
