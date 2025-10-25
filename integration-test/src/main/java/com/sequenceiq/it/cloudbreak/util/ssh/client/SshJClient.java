package com.sequenceiq.it.cloudbreak.util.ssh.client;

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Service
public class SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClient.class);

    private static final int TIMEOUT = 120000;

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFilePath;

    @PostConstruct
    private void logSetup() {
        if (StringUtils.isEmpty(defaultPrivateKeyFilePath)) {
            LOGGER.info("Private key is not set");
        } else {
            if (Files.exists(Path.of(defaultPrivateKeyFilePath))) {
                LOGGER.info("Private key is configured properly: {}", defaultPrivateKeyFilePath);
            } else {
                LOGGER.info("Private key is set but not exists: {}", defaultPrivateKeyFilePath);
            }
        }
    }

    @Retryable(retryFor = net.schmizz.sshj.userauth.UserAuthException.class)
    public SSHClient createSshClient(String host, String user, String password, String privateKeyFilePath) throws IOException {

        LOGGER.info("Initializing SSHJ Client!");
        try {
            Class<?> bcFipsClass = Class.forName("org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider");
            Security.addProvider((java.security.Provider) bcFipsClass.getDeclaredConstructor().newInstance());
            LOGGER.info("Injected BouncyCastle-FIPS provider as a workaround for SSHJ... Fingers crossed!");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("BouncyCastle FIPS not found on the classpath. Falling back, but the test case will probably fail.");
        } catch (Exception e) {
            LOGGER.warn("Exception during the attempt to initialize BouncyCastle FIPS - the test case will probably fail.", e);
        }

        SSHClient client = new SSHClient();

        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.setConnectTimeout(TIMEOUT);
        client.setTimeout(TIMEOUT);
        client.getConnection().setTimeoutMs(TIMEOUT);
        client.getTransport().setTimeoutMs(TIMEOUT);
        client.connect(host, 22);

        if (StringUtils.isBlank(user) && StringUtils.isBlank(privateKeyFilePath)) {
            LOGGER.info("Creating SSH client on '{}' host with 'cloudbreak' user and defaultPrivateKeyFile from application.yml.", host);
            client.authPublickey("cloudbreak", defaultPrivateKeyFilePath);
            Log.log(LOGGER, format(" SSH client has been authenticated with 'cloudbreak' user and key file: [%s] at [%s] host. ", client.isAuthenticated(),
                    client.getRemoteHostname()));
        } else if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(privateKeyFilePath)) {
            LOGGER.info("Creating SSH client on '{}' host with user: '{}' and key file: '{}'.", host, user, privateKeyFilePath);
            client.authPublickey(user, privateKeyFilePath);
            Log.log(LOGGER, format(" SSH client has been authenticated with user (%s) and key file: [%s] at [%s] host. ", user, client.isAuthenticated(),
                    client.getRemoteHostname()));
        } else if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            LOGGER.info("Creating SSH client on '{}' host with user: '{}' and password: '{}'.", host, user, password);
            client.authPassword(user, password);
            Log.log(LOGGER, format(" SSH client has been authenticated with user (%s) and password: [%s] at [%s] host. ", user, client.isAuthenticated(),
                    client.getRemoteHostname()));
        } else {
            LOGGER.error("Creating SSH client is not possible, because of host: '{}', user: '{}', password: '{}' and privateKey: '{}' are missing!",
                    host, user, password, privateKeyFilePath);
            throw new TestFailException(format("Creating SSH client is not possible, because of host: '%s', user: '%s', password: '%s'" +
                            " and privateKey: '%s' are missing!", host, user, password, privateKeyFilePath));
        }
        return client;
    }

    public void upload(SSHClient ssh, String sourceFilePath, String destinationPath) throws IOException {
        LOGGER.info("Waiting for [{}] file to be uploaded to [{}]...", sourceFilePath, destinationPath);
        ssh.setTimeout(TIMEOUT);
        ssh.newSCPFileTransfer().upload(sourceFilePath, destinationPath);
    }

    public void download(SSHClient ssh, String sourceFilePath, String destinationPath) throws IOException {
        LOGGER.info("Waiting for [{}] file to be downloaded to [{}]...", sourceFilePath, destinationPath);
        ssh.setTimeout(TIMEOUT);
        ssh.newSCPFileTransfer().download(sourceFilePath, destinationPath);
    }

    public void uploadToHost(String instanceIP, String sourceFile, String destinationPath) {
        try (SSHClient sshClient = createSshClient(instanceIP, null, null, null)) {
            upload(sshClient, sourceFile, destinationPath);
            Log.log(LOGGER, format("File upload [%s] to host [%s] has been done.", sourceFile, instanceIP));
        } catch (Exception e) {
            Log.error(LOGGER, format("File upload [%s] to host [%s] is failing! %s", sourceFile, instanceIP, e.getMessage()));
            throw new TestFailException(format("File upload [%s] to host [%s] is failing!", sourceFile, instanceIP), e);
        }
    }

    public Pair<Integer, String> execute(SSHClient ssh, String command) throws IOException {
        return execute(ssh, command, 10L);
    }

    @Retryable(retryFor = IOException.class)
    public Pair<Integer, String> execute(SSHClient ssh, String command, long timeoutInSec) throws IOException {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh);
            Command cmd = session.exec(command);
            OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
            Log.log(LOGGER, format("The following SSH command [%s] is going to be executed on host [%s]...", command,
                    ssh.getConnection().getTransport().getRemoteHost()));
            cmd.join(timeoutInSec, TimeUnit.SECONDS);
            return Pair.of(cmd.getExitStatus(), os.toString());
        } catch (Exception ex) {
            LOGGER.info("Exception during ssh command execution", ex);
            throw ex;
        }
    }

    public Map<String, Pair<Integer, String>> executeCommands(Set<String> ipAddresses, String command) {
        Map<String, Pair<Integer, String>> results = new HashMap<>();
        ipAddresses.forEach(ipAddress -> results.put(ipAddress, executeCommand(ipAddress, command)));
        return results;
    }

    public Pair<Integer, String> executeCommand(String instanceIP, String command) {
        try (SSHClient sshClient = createSshClient(instanceIP, null, null, null)) {
            Pair<Integer, String> cmdOut = execute(sshClient, command);
            Log.log(LOGGER, format("Command exit status [%s] and result [%s].", cmdOut.getKey(), cmdOut.getValue()));
            return cmdOut;
        } catch (Exception e) {
            LOGGER.error("SSH fail on [{}] while executing command [{}]. {}", instanceIP, command, e.getMessage());
            throw new TestFailException(" SSH fail on [" + instanceIP + "] while executing command [" + command + "].", e);
        }
    }

    private Session startSshSession(SSHClient ssh) throws ConnectionException, TransportException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }
}
