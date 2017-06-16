package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    private static final int SETUP_TIMEOUT = 180;

    private static final int SSH_POLLING_INTERVAL = 5000;

    private static final int SSH_MAX_ATTEMPTS_FOR_HOSTS = 100;

    @Inject
    private ServiceProviderConnectorAdapter connector;

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

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Value("#{'${cb.cert.dir:}/${cb.tls.cert.file:}'}")
    private String tlsCertificatePath;

    public void setupTls(Stack stack, InstanceMetaData gwInstance, String user, Set<String> sshFingerprints) throws CloudbreakException {
        String publicIp = gatewayConfigService.getGatewayIp(stack, gwInstance);
        int sshPort = gwInstance.getSshPort();
        LOGGER.info("SSHClient parameters: stackId: {}, publicIp: {},  user: {}", stack.getId(), publicIp, user);
        if (publicIp == null) {
            throw new CloudbreakException("Failed to connect to host, IP address not defined.");
        }
        SSHClient ssh = new SSHClient();
        Orchestrator orchestrator = stack.getOrchestrator();
        HostKeyVerifier hostKeyVerifier = new VerboseHostKeyVerifier(sshFingerprints);
        try {
            waitForSsh(stack, publicIp, sshPort, hostKeyVerifier, user);
            String privateKeyLocation = tlsSecurityService.getSshPrivateFileLocation(stack.getId());
            setupTemporarySsh(ssh, publicIp, sshPort, hostKeyVerifier, user, privateKeyLocation, stack.getCredential());
            uploadTlsSetupScript(orchestrator, ssh, publicIp, stack.getGatewayPort(), stack.getCredential());
            executeTlsSetupScript(ssh);
            downloadAndSavePrivateKey(stack, ssh, gwInstance);
        } catch (IOException e) {
            throw new CloudbreakException("Failed to setup TLS through temporary SSH.", e);
        } catch (TemplateException e) {
            throw new CloudbreakException("Failed to generate TLS setup script.", e);
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                throw new CloudbreakException("Couldn't disconnect temp SSH session", e);
            }
        }
    }

    public void removeTemporarySShKey(Stack stack, String publicIp, int sshPort, String user, Set<String> sshFingerprints) throws CloudbreakException {
        SSHClient ssh = new SSHClient();
        try {
            String privateKeyLocation = tlsSecurityService.getSshPrivateFileLocation(stack.getId());
            HostKeyVerifier hostKeyVerifier = new VerboseHostKeyVerifier(sshFingerprints);
            prepareSshConnection(ssh, publicIp, sshPort, hostKeyVerifier, user, privateKeyLocation, stack.getCredential());
            removeTemporarySShKey(ssh, user, stack.getCredential());
        } catch (IOException e) {
            LOGGER.info("Unable to delete temporary SSH key for stack {}", stack.getId());
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                throw new CloudbreakException("Couldn't disconnect temp SSH session", e);
            }
        }
    }

    private void waitForSsh(Stack stack, String publicIp, int sshPort, HostKeyVerifier hostKeyVerifier, String user) throws CloudbreakSecuritySetupException {
        sshCheckerTaskContextPollingService.pollWithTimeoutSingleFailure(
                sshCheckerTask,
                new SshCheckerTaskContext(stack, hostKeyVerifier, publicIp, sshPort, user, tlsSecurityService.getSshPrivateFileLocation(stack.getId())),
                SSH_POLLING_INTERVAL,
                SSH_MAX_ATTEMPTS_FOR_HOSTS);
    }

    private void setupTemporarySsh(SSHClient ssh, String ip, int port, HostKeyVerifier hostKeyVerifier, String user, String privateKeyLocation,
            Credential credential) throws IOException {
        LOGGER.info("Setting up temporary ssh...");
        prepareSshConnection(ssh, ip, port, hostKeyVerifier, user, privateKeyLocation, credential);
        String remoteTlsCertificatePath = "/tmp/cb-client.pem";
        ssh.newSCPFileTransfer().upload(tlsCertificatePath, remoteTlsCertificatePath);
        LOGGER.info("Temporary ssh setup finished succesfully, public key is uploaded to {}", remoteTlsCertificatePath);
    }

    private void prepareSshConnection(SSHClient ssh, String ip, int port, HostKeyVerifier hostKeyVerifier, String user, String privateKeyLocation,
            Credential credential) throws IOException {
        ssh.addHostKeyVerifier(hostKeyVerifier);
        ssh.connect(ip, port);
        if (credential.passwordAuthenticationRequired()) {
            ssh.authPassword(user, credential.getLoginPassword());
        } else {
            ssh.authPublickey(user, privateKeyLocation);
        }
    }

    private void uploadTlsSetupScript(Orchestrator orchestrator, SSHClient ssh, String publicIp, Integer sslPort, Credential credential)
            throws IOException, TemplateException, CloudbreakException {
        LOGGER.info("Uploading tls-setup.sh to the gateway...");
        Map<String, Object> model = new HashMap<>();
        model.put("publicIp", publicIp);
        model.put("username", credential.getLoginUserName());
        model.put("sudopre", credential.passwordAuthenticationRequired() ? String.format("echo '%s'|", credential.getLoginPassword()) : "");
        model.put("sudocheck", credential.passwordAuthenticationRequired() ? "-S" : "");
        model.put("sslPort", sslPort.toString());


        OrchestratorType type = orchestratorTypeResolver.resolveType(orchestrator.getType());

        String tls = processTemplateIntoString(
                freemarkerConfiguration.getTemplate(String.format("init/%s/tls-setup.sh", type.name().toLowerCase()), "UTF-8"), model);
        InMemorySourceFile tlsFile = uploadParameterFile(tls, "tls-setup.sh");
        ssh.newSCPFileTransfer().upload(tlsFile, "/tmp/tls-setup.sh");
        LOGGER.info("tls-setup.sh uploaded to /tmp/tls-setup.sh. Content: {}", tls);

        if (type.hostOrchestrator()) {
            String nginxConf = FileReaderUtils.readFileFromClasspath("init/host/ssl.conf");
            InMemorySourceFile nginxConfFile = uploadParameterFile(nginxConf, "ssl.conf");
            ssh.newSCPFileTransfer().upload(nginxConfFile, "/tmp/ssl.conf");
            LOGGER.info("nginx conf uploaded to /tmp/ssl.conf. Content: {}", nginxConf);
        }
    }

    private InMemorySourceFile uploadParameterFile(String generatedTemplate, final String name) {
        final byte[] tlsScriptBytes = generatedTemplate.getBytes(StandardCharsets.UTF_8);
        return new InMemorySourceFile() {
            @Override
            public String getName() {
                return name;
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
    }

    private void executeTlsSetupScript(SSHClient ssh) throws IOException, CloudbreakException {
        LOGGER.info("Executing tls-setup.sh on the gateway...");
        int exitStatus = executeSshCommand(ssh, "bash /tmp/tls-setup.sh", true, "tls-setup");
        LOGGER.info("tls-setup.sh finished with {} exitcode.", exitStatus);
        if (exitStatus != 0) {
            throw new CloudbreakException(String.format("TLS setup script exited with error code: %s", exitStatus));
        }
    }

    private void removeTemporarySShKey(SSHClient ssh, String user, Credential credential) throws IOException, CloudbreakException {
        if (!credential.passwordAuthenticationRequired()) {
            LOGGER.info("Removing temporary sshkey from the gateway...");
            String removeCommand = String.format("sudo sed -i '/#tmpssh_start/,/#tmpssh_end/{s/./ /g}' /home/%s/.ssh/authorized_keys", user);
            int exitStatus = executeSshCommand(ssh, removeCommand, false, "");
            LOGGER.info("Temporary sshkey removed from the gateway, exitcode: {}", exitStatus);
            if (exitStatus != 0) {
                throw new CloudbreakException(String.format("Failed to remove temp SSH key. Error code: %s", exitStatus));
            }
        }
    }

    private void downloadAndSavePrivateKey(Stack stack, SSHClient ssh, InstanceMetaData gwInstance) throws IOException, CloudbreakSecuritySetupException {
        long stackId = stack.getId();
        String serverCertDir = tlsSecurityService.createServerCertDir(stackId, gwInstance);
        LOGGER.info("Server cert directory is created at: " + serverCertDir);
        ssh.newSCPFileTransfer().download("/tmp/cluster.pem", serverCertDir + "/ca.pem");
        InstanceMetaData metaData = instanceMetaDataRepository.findOne(gwInstance.getId());
        metaData.setServerCert(BaseEncoding.base64().encode(tlsSecurityService.readServerCert(stackId, gwInstance).getBytes()));
        instanceMetaDataRepository.save(metaData);
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
        LOGGER.info(IOUtils.readFully(command.getInputStream()).toString());
        LOGGER.info("Standard error of {} command", commandDesc);
        LOGGER.info(IOUtils.readFully(command.getErrorStream()).toString());
    }
}
