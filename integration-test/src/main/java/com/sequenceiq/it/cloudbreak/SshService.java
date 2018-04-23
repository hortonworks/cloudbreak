package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class SshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshService.class);

    public void executeCommand(List<String> publicIps, String [] files, String pirvateKey, String sshCommand, String sshUser, int sshTimeout, Integer require,
            Map<String,
                    List<String>> sshCheckMap) throws Exception {
        Collection<Future<?>> futures = new ArrayList<>(publicIps.size() * files.length);
        ExecutorService executorService = Executors.newFixedThreadPool(publicIps.size());
        AtomicInteger count = new AtomicInteger(0);
        try {
            for (String file : files) {
                if (sshCheckMap.containsKey("beginsWith")) {
                    sshCheckMap.put("beginsWith", Collections.singletonList(file));
                }
                for (String ip : publicIps) {
                    futures.add(executorService.submit(() -> {
                        try {
                            if (isExecutedCommand(ip, pirvateKey, sshCommand + file, sshUser, sshTimeout, sshCheckMap)) {
                                count.incrementAndGet();
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error occurred during ssh execution: " + e);
                        }
                    }));
                }
                if (sshCheckMap.containsKey("beginsWith")) {
                    sshCheckMap.put("beginsWith", Collections.singletonList(""));
                }
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executorService.shutdown();
        }
        org.testng.Assert.assertEquals(count.get(), require.intValue(), "The number of existing files is different than required.");
    }

    public static boolean isExecutedCommand(String host, String defaultPrivateKeyFile, String sshCommand, String sshUser, int sshTimeout,  Map<String,
            List<String>> sshCheckMap)
            throws IOException {
        if (sshCheckMap.isEmpty()) {
            return false;
        }
        try (SSHClient sshClient = new SSHClient()) {
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(host, 22);
            sshClient.setConnectTimeout(sshTimeout);
            sshClient.authPublickey(sshUser, defaultPrivateKeyFile);
            Pair<Integer, String> cmdOut = execute(sshClient, sshCommand);
            LOGGER.info("Ssh command status code and output: " + cmdOut + " on host: " + host);
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
                    cmd.join(10L, TimeUnit.SECONDS);
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
}