package com.sequenceiq.it.cloudbreak.util.ssh.client;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;

@Component
public class SshJClient {

    public static final long DEFAULT_COMMAND_TIMEOUT_SEC = 10L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClient.class);

    private static final int DOWNLOAD_TIMEOUT_MS = 120000;

    @Retryable(retryFor = IOException.class)
    public Pair<Integer, String> execute(SSHClient ssh, String command, long timeoutInSec) throws IOException {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh); Command cmd = session.exec(command); OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
            LOGGER.info(String.format("The following SSH command [%s] is going to be executed on host [%s]...",
                    command, ssh.getConnection().getTransport().getRemoteHost()));
            cmd.join(timeoutInSec, TimeUnit.SECONDS);
            return Pair.of(cmd.getExitStatus(), os.toString());
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

    private Session startSshSession(SSHClient ssh) throws ConnectionException, TransportException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }
}
