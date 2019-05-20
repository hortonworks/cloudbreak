package com.sequenceiq.it.cloudbreak.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class SshUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshUtil.class);

    private SshUtil() {
    }

    public static boolean executeCommand(String host, String defaultPrivateKeyFile, String sshCommand, String sshUser, Map<String, List<String>>  sshCheckMap)
            throws IOException {
        if (sshCheckMap.isEmpty()) {
            return false;
        }
        try (SSHClient sshClient = new SSHClient()) {
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(host, 22);
            sshClient.authPublickey(sshUser, defaultPrivateKeyFile);
            Pair<Integer, String> cmdOut = execute(sshClient, sshCommand);
            LOGGER.info("Ssh command status code and output: " + cmdOut);
            for (Entry<String, List<String>> entry : sshCheckMap.entrySet()) {
                for (String listValue: entry.getValue()) {
                    if (cmdOut.getLeft() != 0 || !checkCommandOutput(cmdOut, entry.getKey(), listValue)) {
                        LOGGER.error("Ssh command output is not proper: " + entry.getKey() + ' ' + listValue);
                        return false;
                    }
                }
            }
        }
        return true;
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
                try (OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
                    cmd.join(10, TimeUnit.SECONDS);
                    return Pair.of(cmd.getExitStatus(), os.toString());
                }
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

    public static Map<String, List<String>> getSshCheckMap(String sshChecker) {
        List<String> sshCheckList = Arrays.asList(sshChecker.split(";"));
        Map<String, List<String>> sshCheckMap = new HashMap<>();
        for (String elem : sshCheckList) {
            String[] tmpList = elem.split(":");
            Assert.assertTrue(tmpList.length > 1);
            sshCheckMap.put(tmpList[0], Arrays.asList(tmpList[1].split(",")));
        }
        return sshCheckMap;
    }
}