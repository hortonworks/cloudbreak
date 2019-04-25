package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

public class SshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshService.class);

    public int countFilesOnHostByExtensionAndPath(@Nonnull List<String> publicIps, @Nonnull String[] paths, Optional<String> defaultCommandChain,
                    String privateKey, String extension, String sshUser, int sshTimeout, int require) throws IOException {
        int quantity = 0;
        for (String publicIp : publicIps) {
            for (String path : paths) {
                String fileListCommand = String.format("find %s -type f -name *.%s", path, extension);
                String formatTextForFullCommand = !defaultCommandChain.isPresent() || defaultCommandChain.get().endsWith(";") ? "%s%s" : "%s;%s";
                String command = String.format(formatTextForFullCommand, defaultCommandChain.isPresent() ? defaultCommandChain : "", fileListCommand);
                try (SSHClient sshClient = createSshClient(publicIp, sshTimeout, sshUser, privateKey)) {
                    Pair<Integer, String> cmdOut = execute(sshClient, command);
                    quantity += Stream.of(cmdOut.getValue().split(System.getProperty("line.separator"))).filter(s -> s != null && s.startsWith("/")).count();
                } catch (UserAuthException | TransportException ignore) {
                    LOGGER.warn("Whoops! SSH fail on {} while getting info for location: {}", publicIp, path);
                }
            }
        }
        return quantity;
    }

    private static SSHClient createSshClient(String host, int sshTimeout, String sshUser, String defaultPrivateKeyFile) throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, 22);
        client.setConnectTimeout(sshTimeout);
        client.authPublickey(sshUser, defaultPrivateKeyFile);
        return client;
    }

    private static Pair<Integer, String> execute(SSHClient ssh, String command) throws IOException {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh);
                    Command cmd = session.exec(command);
                    OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
            LOGGER.info("The following SSH command is going to be executed on host {}: {}", ssh.getConnection().getTransport().getRemoteHost(), command);
            cmd.join(10L, TimeUnit.SECONDS);
            return Pair.of(cmd.getExitStatus(), os.toString());
        }
    }

    private static Session startSshSession(SSHClient ssh) throws IOException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }

}