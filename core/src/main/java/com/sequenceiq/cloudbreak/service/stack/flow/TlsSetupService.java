package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CERT_DIR;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TLS_CERT_FILE;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.util.Base64;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    private static final int SSH_PORT = 22;
    private static final int SETUP_TIMEOUT = 180;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackRepository stackRepository;

    @Value("#{'${cb.cert.dir:" + CB_CERT_DIR + "}' + '/' + '${cb.tls.cert.file:" + CB_TLS_CERT_FILE + "}'}")
    private String tlsCertificatePath;

    public void setupTls(CloudPlatform cloudPlatform, Stack stack, Map<String, Object> setupProperties) throws CloudbreakException {

        InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);

        LOGGER.info("SSH into gateway node to setup certificates on gateway.");
        String sshFingerprint = connector.getSSHFingerprint(stack, gateway.getInstanceId());
        LOGGER.info("Fingerprint has been determined: {}", sshFingerprint);
        setupTls(stack.getId(), gateway.getPublicIp(), connector.getSSHUser(), stack.getCredential().getPublicKey(), sshFingerprint);

    }

    private void setupTls(Long stackId, String publicIp, String user, String publicKey, String sshFingerprint) throws CloudbreakException {
        LOGGER.info("SSHClient parameters: stackId: {}, publicIp: {},  user: {}", stackId, publicIp, user);
        final SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier(sshFingerprint);
            ssh.connect(publicIp, SSH_PORT);
            ssh.authPublickey(user, tlsSecurityService.getSshPrivateFileLocation(stackId));
            String remoteTlsCertificatePath = "/tmp/cb-client.pem";
            ssh.newSCPFileTransfer().upload(tlsCertificatePath, remoteTlsCertificatePath);
            LOGGER.info("Upload to server: {}", remoteTlsCertificatePath);
            final Session tlsSetupSession = ssh.startSession();
            tlsSetupSession.allocateDefaultPTY();
            String tlsSetupScript = FileReaderUtils.readFileFromClasspath("init/tls-setup.sh");
            tlsSetupScript = tlsSetupScript.replace("$PUBLIC_IP", publicIp);
            final Session.Command tlsSetupCmd = tlsSetupSession.exec(tlsSetupScript);
            LOGGER.info("Execute tls-setup.sh: {}", tlsSetupScript);
            tlsSetupCmd.join(SETUP_TIMEOUT, TimeUnit.SECONDS);
            tlsSetupSession.close();
            final Session changeSshKeySession = ssh.startSession();
            changeSshKeySession.allocateDefaultPTY();
            String removeScript = "sudo sed -i '/#tmpssh_start/,/#tmpssh_end/{s/./ /g}' /home/%s/.ssh/authorized_keys";
            final Session.Command tmpSshRemoveCmd = changeSshKeySession.exec(String.format(removeScript, user));
            tmpSshRemoveCmd.join(SETUP_TIMEOUT, TimeUnit.SECONDS);
            changeSshKeySession.close();
            ssh.newSCPFileTransfer().download("/tmp/server.pem", tlsSecurityService.getCertDir(stackId) + "/ca.pem");
            if (tlsSetupCmd.getExitStatus() != 0) {
                throw new CloudbreakException(String.format("TLS setup script exited with error code: %s", tlsSetupCmd.getExitStatus()));
            }
            if (tmpSshRemoveCmd.getExitStatus() != 0) {
                throw new CloudbreakException(String.format("Failed to remove temp SSH key. Error code: %s", tmpSshRemoveCmd.getExitStatus()));
            }
            Stack stackWithSecurity = stackRepository.findByIdWithSecurityConfig(stackId);
            SecurityConfig securityConfig = stackWithSecurity.getSecurityConfig();
            securityConfig.setServerCert(Base64.encodeAsString(tlsSecurityService.readServerCert(stackId).getBytes()));
            securityConfigRepository.save(securityConfig);
        } catch (IOException e) {
            throw new CloudbreakException("Failed to setup TLS through temporary SSH.", e);
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                throw new CloudbreakException(String.format("Couldn't disconnect temp SSH session"), e);
            }
        }
    }

}
