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
    private String defaultPrivateKeyFile;

    public SshJClient() {
    }

    @PostConstruct
    private void logSetup() {
        if (StringUtils.isEmpty(defaultPrivateKeyFile)) {
            LOGGER.info("Private key is not set");
        } else {
            if (Files.exists(Path.of(defaultPrivateKeyFile))) {
                LOGGER.info("Private key is configured properly: {}", defaultPrivateKeyFile);
            } else {
                LOGGER.info("Private key is set but not exists: {}", defaultPrivateKeyFile);
            }
        }
    }

    protected SSHClient createSshClient(String host) throws IOException {
        SSHClient client = new SSHClient();

        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, 22);
        client.setConnectTimeout(120000);
        client.authPublickey("cloudbreak", defaultPrivateKeyFile);
        Log.log(LOGGER, format("SSH client has been authenticated [%s] with at [%s]", client.isAuthenticated(), client.getRemoteHostname()));

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
        try (SSHClient sshClient = createSshClient(instanceIP)) {
            Pair<Integer, String> cmdOut = execute(sshClient, command);
            Log.log(LOGGER, format("Command exit status [%s] and result [%s].", cmdOut.getKey(), cmdOut.getValue()));
            return cmdOut;
        } catch (Exception e) {
            LOGGER.error("SSH fail on [{}] while executing command [{}]. {}", instanceIP, command, e.getMessage());
            throw new TestFailException(" SSH fail on [" + instanceIP + "] while executing command [" + command + "]. " + e.getMessage());
        }
    }

    private Session startSshSession(SSHClient ssh) throws ConnectionException, TransportException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }
}
