package com.sequenceiq.it.cloudbreak.util.ssh.client;

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Service
public class SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClient.class);

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFilePath;

    public SshJClient() {
    }

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

    protected SSHClient createSshClient(String host, String user, String password, String privateKeyFilePath) throws IOException {
        SSHClient client = new SSHClient();

        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, 22);
        client.setConnectTimeout(120000);
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
            throw new TestFailException(String.format("Creating SSH client is not possible, because of host: '%s', user: '%s', password: '%s'" +
                            " and privateKey: '%s' are missing!", host, user, password, privateKeyFilePath));
        }
        return client;
    }

    protected Pair<Integer, String> execute(SSHClient ssh, String command) throws IOException {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh);
            Session.Command cmd = session.exec(command);
            OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
            Log.log(LOGGER, format("The following SSH command [%s] is going to be executed on host [%s]", ssh.getConnection().getTransport().getRemoteHost(),
                    command));
            cmd.join(10L, TimeUnit.SECONDS);
            return Pair.of(cmd.getExitStatus(), os.toString());
        }
    }

    protected Pair<Integer, String> executeCommand(String instanceIP, String command) {
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
