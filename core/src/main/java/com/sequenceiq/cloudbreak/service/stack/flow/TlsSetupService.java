package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CERT_DIR;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TLS_CERT_FILE;
import static java.util.Collections.singletonMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.util.Base64;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;

@Component
public class TlsSetupService {
    public static final int SSH_PORT = 22;

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);
    private static final int SETUP_TIMEOUT = 180;
    private static final int SSH_POLLING_INTERVAL = 5000;
    private static final int SSH_MAX_ATTEMPTS_FOR_HOSTS = 100;

    @Inject
    private CloudPlatformResolver platformResolver;

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

    public void setupTls(CloudPlatform cloudPlatform, Stack stack) throws CloudbreakException {
        InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudPlatformConnector connector = platformResolver.connector(cloudPlatform);
        LOGGER.info("SSH into gateway node to setup certificates on gateway.");
        Set<String> sshFingerprints = connector.getSSHFingerprints(stack, gateway.getInstanceId());
        LOGGER.info("Fingerprint has been determined: {}", sshFingerprints);
        setupTls(stack, gateway.getPublicIp(), connector.getSSHUser(singletonMap(ProvisioningService.PLATFORM, cloudPlatform.name())), sshFingerprints);
    }

    private void setupTls(Stack stack, String publicIp, String user, Set<String> sshFingerprints) throws
            CloudbreakException {
        LOGGER.info("SSHClient parameters: stackId: {}, publicIp: {},  user: {}", stack.getId(), publicIp, user);
        SSHClient ssh = new SSHClient();
        String privateKeyLocation = tlsSecurityService.getSshPrivateFileLocation(stack.getId());
        HostKeyVerifier hostKeyVerifier = new VerboseHostKeyVerifier(sshFingerprints, stack.cloudPlatform());
        try {
            waitForSsh(stack, publicIp, hostKeyVerifier, user, privateKeyLocation);
            setupTemporarySsh(ssh, publicIp, hostKeyVerifier, user, privateKeyLocation);
            uploadTlsSetupScript(ssh, publicIp);
            executeTlsSetupScript(ssh);
            removeTemporarySShKey(ssh, user);
            downloadAndSavePrivateKey(stack, ssh);
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

    private void waitForSsh(Stack stack, String publicIp, HostKeyVerifier hostKeyVerifier, String user, String privateKeyLocation) {
        sshCheckerTaskContextPollingService.pollWithTimeout(
                sshCheckerTask,
                new SshCheckerTaskContext(stack, hostKeyVerifier, publicIp, user, tlsSecurityService.getSshPrivateFileLocation(stack.getId())),
                SSH_POLLING_INTERVAL,
                SSH_MAX_ATTEMPTS_FOR_HOSTS);
    }

    private void setupTemporarySsh(SSHClient ssh, String publicIp, HostKeyVerifier hostKeyVerifier, String user, String privateKeyLocation)
        throws IOException {
        LOGGER.info("Setting up temporary ssh...");
        ssh.addHostKeyVerifier(hostKeyVerifier);
        ssh.connect(publicIp, SSH_PORT);
        ssh.authPublickey(user, privateKeyLocation);
        String remoteTlsCertificatePath = "/tmp/cb-client.pem";
        ssh.newSCPFileTransfer().upload(tlsCertificatePath, remoteTlsCertificatePath);
        LOGGER.info("Temporary ssh setup finished succesfully, public key is uploaded to {}", remoteTlsCertificatePath);
    }

    private void uploadTlsSetupScript(SSHClient ssh, String publicIp) throws IOException {
        LOGGER.info("Uploading tls-setup.sh to the gateway...");
        String tlsSetupScript = FileReaderUtils.readFileFromClasspath("init/tls-setup.sh").replace("$PUBLIC_IP", publicIp);
        final byte[] tlsScriptBytes = tlsSetupScript.getBytes(StandardCharsets.UTF_8);
        InMemorySourceFile scriptFile = new InMemorySourceFile() {
            @Override
            public String getName() {
                return "tls-setup.sh";
            }

            @Override
            public long getLength() {
                return tlsScriptBytes.length;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(tlsScriptBytes);
            }
        };
        ssh.newSCPFileTransfer().upload(scriptFile, "/tmp/tls-setup.sh");
        LOGGER.info("tls-setup.sh uploaded to /tmp/tls-setup.sh. Content: {}", tlsSetupScript);
    }

    private void executeTlsSetupScript(SSHClient ssh) throws IOException, CloudbreakException {
        LOGGER.info("Executing tls-setup.sh on the gateway...");
        int exitStatus = executeSshCommand(ssh, "bash /tmp/tls-setup.sh", true, "tls-setup");
        LOGGER.info("tls-setup.sh finished with {} exitcode.", exitStatus);
        if (exitStatus != 0) {
            throw new CloudbreakException(String.format("TLS setup script exited with error code: %s", exitStatus));
        }
    }

    private void removeTemporarySShKey(SSHClient ssh, String user) throws IOException, CloudbreakException {
        LOGGER.info("Removing temporary sshkey from the gateway...");
        String removeCommand = String.format("sudo sed -i '/#tmpssh_start/,/#tmpssh_end/{s/./ /g}' /home/%s/.ssh/authorized_keys", user);
        int exitStatus = executeSshCommand(ssh, removeCommand, false, "");
        LOGGER.info("Temporary sshkey removed from the gateway, exitcode: {}", exitStatus);
        if (exitStatus != 0) {
            throw new CloudbreakException(String.format("Failed to remove temp SSH key. Error code: %s", exitStatus));
        }
    }

    private void downloadAndSavePrivateKey(Stack stack, SSHClient ssh) throws IOException, CloudbreakSecuritySetupException {
        ssh.newSCPFileTransfer().download("/tmp/server.pem", tlsSecurityService.getCertDir(stack.getId()) + "/ca.pem");
        Stack stackWithSecurity = stackRepository.findByIdWithSecurityConfig(stack.getId());
        SecurityConfig securityConfig = stackWithSecurity.getSecurityConfig();
        securityConfig.setServerCert(Base64.encodeAsString(tlsSecurityService.readServerCert(stack.getId()).getBytes()));
        securityConfigRepository.save(securityConfig);
    }

    private Session startSshSession(SSHClient ssh) throws IOException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }

    private int executeSshCommand(SSHClient ssh, String command, boolean logOutput, String logPrefix) throws IOException {
        Session session = startSshSession(ssh);
        Session.Command cmd = session.exec(command);
        if (logOutput) {
            logStdOutAndStdErr(cmd, logPrefix);
        }
        cmd.join(SETUP_TIMEOUT, TimeUnit.SECONDS);
        session.close();
        return cmd.getExitStatus();
    }

    private void logStdOutAndStdErr(Session.Command command, String commandDesc) throws IOException {
        LOGGER.info("Standard output of {} command", commandDesc);
        LOGGER.info(new String(IOUtils.readFully(command.getInputStream()).toString()));
        LOGGER.info("Standard error of {} command", commandDesc);
        LOGGER.info(new String(IOUtils.readFully(command.getErrorStream()).toString()));
    }
}
