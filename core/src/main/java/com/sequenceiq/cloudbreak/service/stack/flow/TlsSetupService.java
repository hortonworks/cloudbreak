package com.sequenceiq.cloudbreak.service.stack.flow;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.service.ProvisioningSetupService;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    public static final int SSH_PORT = 22;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    public void setupTls(CloudPlatform cloudPlatform, Stack stack, Map<String, Object> setupProperties) {

        InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);

        LOGGER.info("SSH into gateway node to setup certificates on gateway.");
        final SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(connector.getSSHThumbprint(stack, gateway.getInstanceId()));
        try {
            ssh.connect(gateway.getPublicIp(), SSH_PORT);
            ssh.authPublickey(connector.getSSHUser(), (String) setupProperties.get(ProvisioningSetupService.SSH_PRIVATEKEY_LOCATION));
            final Session session = ssh.startSession();
            final Session.Command cmd = session.exec("touch /tmp/alma");
            System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
            cmd.join(5, TimeUnit.SECONDS);
            System.out.println("\n** exit status: " + cmd.getExitStatus());
            session.close();
            ssh.disconnect();
        } catch (IOException e) {
            LOGGER.info("this is very very bad");
        }

    }
}
