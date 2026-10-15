package com.sequenceiq.it.cloudbreak.util.ssh.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;

@Component
public class SshJClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClient.class);

    private static final int DOWNLOAD_TIMEOUT_MS = 120000;

    private static final long EXIT_STATUS_WAIT_SEC = 10L;

    private static final int SESSION_START_ATTEMPTS = 3;

    private static final long SESSION_START_BACKOFF_MS = 1000L;

    private final RetryTemplate sessionStartRetryTemplate = RetryTemplate.builder()
            .maxAttempts(SESSION_START_ATTEMPTS)
            .fixedBackoff(SESSION_START_BACKOFF_MS)
            .retryOn(IOException.class)
            .build();

    public SshCommandOutcome execute(SSHClient ssh, String instanceIp, String command) throws IOException {
        return execute(ssh, instanceIp, command, EXIT_STATUS_WAIT_SEC);
    }

    /**
     * Executes the command and waits until it finishes, then waits at most {@code exitStatusWaitSec} for its exit status. Only starting the
     * session is retried. Once the command has started, nothing is retried, because the command may not be idempotent and may still be running on
     * the instance: an {@link IOException} after that point (e.g. a {@link SocketTimeoutException} when the command produces no output for longer
     * than the channel read timeout configured by {@link SshJClientFactory}, or a {@link ConnectionException} when the exit status does not arrive
     * in time) is thrown to the caller.
     */
    public SshCommandOutcome execute(SSHClient ssh, String instanceIp, String command, long exitStatusWaitSec) throws IOException {
        try (Session session = sessionStartRetryTemplate.execute(context -> startSshSession(ssh))) {
            LOGGER.info("The following SSH command [{}] is going to be executed on host [{}]...", command, ssh.getConnection().getTransport().getRemoteHost());
            try (Command cmd = session.exec(command); ByteArrayOutputStream os = IOUtils.readFully(cmd.getInputStream())) {
                cmd.join(exitStatusWaitSec, TimeUnit.SECONDS);
                return SshCommandOutcome.executed(instanceIp, exitStatusOf(cmd), os.toString(StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            LOGGER.info("Exception during ssh command execution", ex);
            throw ex;
        }
    }

    public void download(SSHClient ssh, String sourceFilePath, String destinationPath) throws IOException {
        LOGGER.info("Waiting for [{}] file to be downloaded to [{}]...", sourceFilePath, destinationPath);
        ssh.setTimeout(DOWNLOAD_TIMEOUT_MS);
        ssh.newSCPFileTransfer().download(sourceFilePath, destinationPath);
    }

    /**
     * SSHJ reports no exit status when the channel closes without one (e.g. the command was killed by a signal). Map it to a non-zero code so
     * callers see a failed command instead of hitting a NullPointerException while unboxing the exit status.
     */
    private int exitStatusOf(Command cmd) {
        Integer exitStatus = cmd.getExitStatus();
        if (exitStatus == null) {
            LOGGER.warn("SSH command finished without an exit status (exit signal: {}), treating it as failed with exit status {}.",
                    cmd.getExitSignal(), SshCommandOutcome.MISSING_RETURN_CODE);
            return SshCommandOutcome.MISSING_RETURN_CODE;
        }
        return exitStatus;
    }

    private Session startSshSession(SSHClient ssh) throws ConnectionException, TransportException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }
}
