package com.sequenceiq.it.cloudbreak.util.ssh.client;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

@Component
public class SshJClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientFactory.class);

    private static final int TIMEOUT = 120000;

    /**
     * How long a command may run without producing any output before it is considered hanging.
     */
    private static final int COMMAND_OUTPUT_IDLE_TIMEOUT_MS = 10 * 60 * 1000;

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

    /**
     * Retries transient connection failures. Authentication failures are not retried: a rejected credential stays rejected, so retrying only
     * delays the failure.
     */
    @Retryable(retryFor = IOException.class, noRetryFor = UserAuthException.class)
    public SSHClient createSshClient(String host, String user, String password, String privateKeyFilePath) throws IOException {
        DefaultConfig config = new DefaultConfig();
        config.setChannelReadTimeoutMs(COMMAND_OUTPUT_IDLE_TIMEOUT_MS);
        SSHClient client = new SSHClient(config);
        try {
            return connectAndAuthenticate(client, host, user, password, privateKeyFilePath);
        } catch (IOException | RuntimeException e) {
            closeQuietly(client, host);
            throw e;
        }
    }

    private SSHClient connectAndAuthenticate(SSHClient client, String host, String user, String password, String privateKeyFilePath) throws IOException {
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
            LOGGER.info("Creating SSH client on '{}' host with user: '{}' and a password.", host, user);
            client.authPassword(user, password);
            Log.log(LOGGER, format(" SSH client has been authenticated with user (%s) and password: [%s] at [%s] host. ", user, client.isAuthenticated(),
                    client.getRemoteHostname()));
        } else {
            LOGGER.error("Creating SSH client is not possible on host '{}': neither a private key nor a password is given for user '{}'!", host, user);
            throw new TestFailException(format("Creating SSH client is not possible on host '%s': neither a private key nor a password is given " +
                    "for user '%s'!", host, user));
        }
        return client;
    }

    private void closeQuietly(SSHClient client, String host) {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.debug("Failed to close the SSH client of host '{}' after a failed connection attempt.", host, e);
        }
    }
}
