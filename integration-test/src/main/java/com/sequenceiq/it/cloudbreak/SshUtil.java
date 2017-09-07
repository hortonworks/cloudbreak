package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;


public class SshUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.SshUtil.class);

    private SshUtil() {
    }

    public static boolean executeCommand(String host, String defaultPrivateKeyFile, String sshCommand, String checkType, String value) throws IOException {
        try (SSHClient sshClient = new SSHClient()) {
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(host, 22);
            sshClient.authPublickey("cloudbreak", defaultPrivateKeyFile);
            Pair<Integer, String> cmdOut = execute(sshClient, sshCommand);
            LOGGER.info("Ssh command status code and output: " + cmdOut);
            return cmdOut.getLeft() == 0 && checkCommandOutput(cmdOut, checkType, value);
        }
    }

    private static Session startSshSession(SSHClient ssh) throws IOException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }

    private static Pair<Integer, String> execute(SSHClient ssh, String command) throws IOException {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh)) {
            try (Command cmd = session.exec(command)) {
                String stdout = IOUtils.readFully(cmd.getInputStream()).toString();
                cmd.join(10, TimeUnit.SECONDS);
                return Pair.of(cmd.getExitStatus(), stdout);
            }
        }
    }

    private static boolean checkCommandOutput(Pair<Integer, String> cmdOut, String checkType, String value) {
        switch (checkType) {
            case "contains":
                return cmdOut.getRight().contains(value);
            case "notContains":
                return !(cmdOut.getRight().contains(value));
            case "beginsWith":
                return cmdOut.getRight().startsWith(value);
            default:
                LOGGER.info("Check type {} is not exist!", checkType);
                break;
        }
        return false;
    }
}