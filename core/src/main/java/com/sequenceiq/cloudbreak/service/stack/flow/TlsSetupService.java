package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CERT_DIR;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TLS_CERT_FILE;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
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
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Component
public class TlsSetupService {

    public static final int SSH_PORT = 22;

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);
    private static final int SETUP_TIMEOUT = 180;
    private static final int SSH_POLLING_INTERVAL = 5000;
    private static final int SSH_MAX_ATTEMPTS_FOR_HOSTS = 100;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private PollingService<SshCheckerTaskContext> sshCheckerTaskContextPollingService;

    @Inject
    private SshCheckerTask sshCheckerTask;

    @Value("#{'${cb.cert.dir:" + CB_CERT_DIR + "}' + '/' + '${cb.tls.cert.file:" + CB_TLS_CERT_FILE + "}'}")
    private String tlsCertificatePath;

    public void setupTls(CloudPlatform cloudPlatform, Stack stack, Map<String, Object> setupProperties) throws CloudbreakException {

        InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);
        LOGGER.info("SSH into gateway node to setup certificates on gateway.");
        Set<String> sshFingerprints = connector.getSSHFingerprints(stack, gateway.getInstanceId());
        LOGGER.info("Fingerprint has been determined: {}", sshFingerprints);
        setupTls(cloudPlatform, stack, gateway.getPublicIp(), connector.getSSHUser(), stack.getCredential().getPublicKey(), sshFingerprints);

    }

    private void setupTls(CloudPlatform cloudPlatform, Stack stack, String publicIp, String user, String publicKey, Set<String> sshFingerprints) throws
            CloudbreakException {
        LOGGER.info("SSHClient parameters: stackId: {}, publicIp: {},  user: {}", stack.getId(), publicIp, user);
        final SSHClient ssh = new SSHClient();
        try {
            HostKeyVerifier hostKeyVerifier;
            if (cloudPlatform == CloudPlatform.AWS) {
                hostKeyVerifier = new PromiscuousVerifier();
            } else {
                hostKeyVerifier = new VerboseHostKeyVerifier(sshFingerprints);
            }

            sshCheckerTaskContextPollingService.pollWithTimeout(
                    sshCheckerTask,
                    new SshCheckerTaskContext(stack, hostKeyVerifier, publicIp, user, tlsSecurityService.getSshPrivateFileLocation(stack.getId())),
                    SSH_POLLING_INTERVAL,
                    SSH_MAX_ATTEMPTS_FOR_HOSTS);

            ssh.addHostKeyVerifier(hostKeyVerifier);
            ssh.connect(publicIp, SSH_PORT);
            ssh.authPublickey(user, tlsSecurityService.getSshPrivateFileLocation(stack.getId()));
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
            ssh.newSCPFileTransfer().download("/tmp/server.pem", tlsSecurityService.getCertDir(stack.getId()) + "/ca.pem");
            if (tlsSetupCmd.getExitStatus() != 0) {
                throw new CloudbreakException(String.format("TLS setup script exited with error code: %s", tlsSetupCmd.getExitStatus()));
            }
            if (tmpSshRemoveCmd.getExitStatus() != 0) {
                throw new CloudbreakException(String.format("Failed to remove temp SSH key. Error code: %s", tmpSshRemoveCmd.getExitStatus()));
            }
            Stack stackWithSecurity = stackRepository.findByIdWithSecurityConfig(stack.getId());
            SecurityConfig securityConfig = stackWithSecurity.getSecurityConfig();
            securityConfig.setServerCert(Base64.encodeAsString(tlsSecurityService.readServerCert(stack.getId()).getBytes()));
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
