package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.sequenceiq.cloudbreak.core.bootstrap.service.ContainerOrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ContainerOrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
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
    public static final int SSH_PORT = 22;

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
    private ContainerOrchestratorTypeResolver containerOrchestratorTypeResolver;

    @Value("#{'${cb.cert.dir:}/${cb.tls.cert.file:}'}")
    private String tlsCertificatePath;

    public void setupTls(Stack stack, String publicIp, String user, Set<String> sshFingerprints) throws CloudbreakException {
        LOGGER.info("SSHClient parameters: stackId: {}, publicIp: {},  user: {}", stack.getId(), publicIp, user);
        if (publicIp == null) {
            throw new CloudbreakException("Failed to connect to host, IP address not defined.");
        }
        SSHClient ssh = new SSHClient();
        Orchestrator orchestrator = stack.getOrchestrator();
        String privateKeyLocation = tlsSecurityService.getSshPrivateFileLocation(stack.getId());
        HostKeyVerifier hostKeyVerifier = new VerboseHostKeyVerifier(sshFingerprints);
        try {
            waitForSsh(stack, publicIp, hostKeyVerifier, user, privateKeyLocation);
            setupTemporarySsh(ssh, publicIp, hostKeyVerifier, user, privateKeyLocation, stack.getCredential());
            uploadTlsSetupScript(orchestrator, ssh, publicIp, stack.getGatewayPort(), stack.getCredential());
            uploadSaltConfig(orchestrator, ssh);
            executeTlsSetupScript(ssh);
            removeTemporarySShKey(ssh, user, stack.getCredential());
            downloadAndSavePrivateKey(stack, ssh);
        } catch (IOException e) {
            throw new CloudbreakException("Failed to setup TLS through temporary SSH.", e);
        } catch (TemplateException e) {
            throw new CloudbreakException("Failed to generate TLS setup script.", e);
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                throw new CloudbreakException(String.format("Couldn't disconnect temp SSH session"), e);
            }
        }
    }

    private void waitForSsh(Stack stack, String publicIp, HostKeyVerifier hostKeyVerifier, String user, String privateKeyLocation) {
        sshCheckerTaskContextPollingService.pollWithTimeoutSingleFailure(
                sshCheckerTask,
                new SshCheckerTaskContext(stack, hostKeyVerifier, publicIp, user, tlsSecurityService.getSshPrivateFileLocation(stack.getId())),
                SSH_POLLING_INTERVAL,
                SSH_MAX_ATTEMPTS_FOR_HOSTS);
    }

    private void setupTemporarySsh(SSHClient ssh, String ip, HostKeyVerifier hostKeyVerifier, String user, String privateKeyLocation, Credential credential)
            throws IOException {
        LOGGER.info("Setting up temporary ssh...");
        ssh.addHostKeyVerifier(hostKeyVerifier);
        ssh.connect(ip, SSH_PORT);
        if (credential.passwordAuthenticationRequired()) {
            ssh.authPassword(user, credential.getLoginPassword());
        } else {
            ssh.authPublickey(user, privateKeyLocation);
        }
        String remoteTlsCertificatePath = "/tmp/cb-client.pem";
        ssh.newSCPFileTransfer().upload(tlsCertificatePath, remoteTlsCertificatePath);
        LOGGER.info("Temporary ssh setup finished succesfully, public key is uploaded to {}", remoteTlsCertificatePath);
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


        ContainerOrchestratorType type = containerOrchestratorTypeResolver.resolveType(orchestrator.getType());

        String tls = processTemplateIntoString(
                freemarkerConfiguration.getTemplate(String.format("init/%s/tls-setup.sh", type.name().toLowerCase()), "UTF-8"), model);
        InMemorySourceFile tlsFile = uploadParameterFile(tls, "tls-setup.sh");
        ssh.newSCPFileTransfer().upload(tlsFile, "/tmp/tls-setup.sh");
        LOGGER.info("tls-setup.sh uploaded to /tmp/tls-setup.sh. Content: {}", tls);

        if (type.hostOrchestrator()) {
            String nginxConf = FileReaderUtils.readFileFromClasspath("init/host/nginx.conf");
            InMemorySourceFile nginxConfFile = uploadParameterFile(nginxConf, "nginx.conf");
            ssh.newSCPFileTransfer().upload(nginxConfFile, "/tmp/nginx.conf");
            LOGGER.info("nginx conf uploaded to /tmp/nginx.conf. Content: {}", nginxConf);

            String notAv = FileReaderUtils.readFileFromClasspath("init/host/50x.json");
            InMemorySourceFile notAvFile = uploadParameterFile(notAv, "50x.json");
            ssh.newSCPFileTransfer().upload(notAvFile, "/tmp/50x.json");
            LOGGER.info("ngingx error page uploaded to /tmp/50x.json. Content: {}", notAv);
        }
    }

    private void uploadSaltConfig(Orchestrator orchestrator, SSHClient ssh) throws CloudbreakException, IOException {
        if (containerOrchestratorTypeResolver.resolveType(orchestrator.getType()).hostOrchestrator()) {
            // TODO generate tar or zip on-the-fly
            ByteArrayOutputStream byteArrayOutputStream = IOUtils.readFully(getClass().getResourceAsStream("/salt/salt.tar.gz"));
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            LOGGER.info("Upload salt.tar.gz to /tmp/salt.tar.gz");
            ssh.newSCPFileTransfer().upload(new InMemorySourceFile() {
                @Override
                public String getName() {
                    return "salt.tar.gz";
                }

                @Override
                public long getLength() {
                    return byteArray.length;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(byteArray);
                }
            }, "/tmp/salt.tar.gz");
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

    private void downloadAndSavePrivateKey(Stack stack, SSHClient ssh) throws IOException, CloudbreakSecuritySetupException {
        ssh.newSCPFileTransfer().download("/tmp/server.pem", tlsSecurityService.getCertDir(stack.getId()) + "/ca.pem");
        Stack stackWithSecurity = stackRepository.findByIdWithSecurityConfig(stack.getId());
        SecurityConfig securityConfig = stackWithSecurity.getSecurityConfig();
        securityConfig.setServerCert(BaseEncoding.base64().encode(tlsSecurityService.readServerCert(stack.getId()).getBytes()));
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
