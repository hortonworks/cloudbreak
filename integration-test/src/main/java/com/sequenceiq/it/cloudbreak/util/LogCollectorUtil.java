package com.sequenceiq.it.cloudbreak.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Service
public class LogCollectorUtil {

    public static final String TMP_LOGS = "/tmp/logs";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCollectorUtil.class);

    private static final Map<String, List<String>> LOG_FILES_TO_COLLECT_ON_ISSUE =
            Map.of("There are missing nodes from salt network response", List.of("/var/log/saltboot.log", "/var/log/nginx/access.log", "/var/log/messages"),
                    "SocketTimeoutException", List.of("/var/log/salt/master", "/var/log/salt/minion", "/var/log/saltboot.log", "/var/log/messages"));

    @Value("${integrationtest.outputdir:.}")
    private String workingDirectory;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private SshJClient sshJClient;

    public void collectLogFiles(String statusReason, List<String> ipAddresses) {
        LOGGER.info("Collecting logs for status reason: {}", statusReason);

        for (String issue : LOG_FILES_TO_COLLECT_ON_ISSUE.keySet()) {
            if (!StringUtils.isEmpty(statusReason) && statusReason.contains(issue)) {
                for (String logFilePath : LOG_FILES_TO_COLLECT_ON_ISSUE.get(issue)) {
                    try {
                        sshJClientActions.executeSshCommandOnHosts(ipAddresses, "sudo mkdir -p " + TMP_LOGS
                                + "; sudo rsync -R " + logFilePath + " " + TMP_LOGS + "/" + "; sudo chown -R cloudbreak:cloudbreak " + TMP_LOGS);
                        for (String ipAddress : ipAddresses) {
                            try (SSHClient sshClient = sshJClient.createSshClient(ipAddress, null, null, null)) {
                                String downloadPath = workingDirectory + "/debug-logs/" + ipAddress + logFilePath;
                                FileUtils.createParentDirectories(new File(downloadPath));
                                sshJClient.download(sshClient, TMP_LOGS + logFilePath,
                                        workingDirectory + "/debug-logs/" + ipAddress + logFilePath);
                            } catch (IOException e) {
                                LOGGER.warn("Failed to create ssh client for {}. Reason: {}", ipAddress, e.getMessage(), e);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to collect {} from all hosts. Reason: {}", logFilePath, e.getMessage(), e);
                    }
                }
            }
        }
    }
}