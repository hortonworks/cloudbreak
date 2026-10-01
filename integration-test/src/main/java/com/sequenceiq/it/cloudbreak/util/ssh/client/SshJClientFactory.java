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

import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Component
public class SshJClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientFactory.class);

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

    @Retryable(retryFor = IOException.class)
    public SSHClient createSshClient(String host, String user, String password, String privateKeyFilePath) throws IOException {
        LOGGER.info("Initializing SSHJ Client!");
        try {
            BouncyCastleFipsProviderLoader.load();
            LOGGER.info("Injected BouncyCastle-FIPS provider as a workaround for SSHJ... Fingers crossed!");
        } catch (Exception e) {
            LOGGER.warn("Exception during the attempt to initialize BouncyCastle FIPS - the test case will probably fail.", e);
        }

        SSHClient client = new SSHClient(new DefaultConfig());

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
}
