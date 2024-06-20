package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.String.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class ScpDownloadClusterLogsActions {

    private static final String LOG_FOLDER_NAME = "yarn_cluster_logs";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScpDownloadClusterLogsActions.class);

    @Value("${integrationtest.outputdir:.}")
    private String workingDirectory;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private SshJClient sshJClient;

    public void downloadClusterLogs(String environmentCrn, String resourceName, Map<HostGroupType, String> ips, String serviceName) {
        downloadClusterLogs(environmentCrn, resourceName, ips, workingDirectory, serviceName);
    }

    public void downloadClusterLogs(String environmentCrn, String resourceName, Map<HostGroupType, String> ips,
            String workingDirectoryLocation, String serviceName) {
        try {
            if (!ips.isEmpty()) {
                ips.forEach((hostGroupType, ip) -> {
                    copyLogsToCloudbreak(ip, "/var/log/", "var-logs");
                    downloadLogs(hostGroupType, ip, workingDirectoryLocation, serviceName, "var-logs.tar.gz");
                    if (!StringUtils.equalsIgnoreCase("freeipa", serviceName)) {
                        copyLogsToCloudbreak(ip, "/var/run/cloudera-scm-agent/process/", "cm-logs");
                        downloadLogs(hostGroupType, ip, workingDirectoryLocation, serviceName, "cm-logs.tar.gz");
                    }
                });
            } else {
                Log.warn(LOGGER, format(" IPs are missing! Cluster log collection is not possible right now for resource: %s ",
                        resourceName));
            }
        } catch (Exception e) {
            Log.warn(LOGGER, format(" Error occurred while try to retrieve cluster logs! Instances: %s | Exception: %s ",
                    ips, e.getMessage()));
        }
    }

    private void copyLogsToCloudbreak(String instanceIp, String sourceFilePath, String destinationDirectory) {
        String mkdirLogsDirectoryCommand = format("mkdir -p %s", destinationDirectory);
        sshJClientActions.executeSshCommand(instanceIp, mkdirLogsDirectoryCommand);
        sshJClientActions.executeSshCommand(instanceIp, format("sudo cp -ru %s %s/ && sudo chown -R cloudbreak:cloudbreak %s", sourceFilePath,
                destinationDirectory, destinationDirectory));
        sshJClientActions.executeSshCommand(instanceIp, format("tar -czf %s.tar.gz %s && rm -rf %s", destinationDirectory, destinationDirectory,
                destinationDirectory));
    }

    private void download(String instanceIp, String sourceFilePath, String destinationPath) {
        download(instanceIp, null, null, null, sourceFilePath, destinationPath);
    }

    private void download(String instanceIp, String user, String password, String privateKeyFilePath, String sourceFilePath, String destinationPath) {
        try (SSHClient sshClient = sshJClient.createSshClient(instanceIp, user, password, privateKeyFilePath)) {
            sshJClient.download(sshClient, sourceFilePath, destinationPath);
            Log.log(LOGGER, format("File download [%s] from host [%s] has been done.", sourceFilePath, instanceIp));
        } catch (Exception e) {
            Log.error(LOGGER, format("File download [%s] from host [%s] is failing! %s", sourceFilePath, instanceIp, e.getMessage()));
            throw new TestFailException(format("File download [%s] from host [%s] is failing!", sourceFilePath, instanceIp), e);
        }
    }

    private void downloadLogs(HostGroupType hostGroupType, String instanceIp, String workingDirectoryLocation, String serviceName, String logPath) {
        String destinationPath = String.join("/", workingDirectoryLocation, LOG_FOLDER_NAME, serviceName, hostGroupType.getName() + '-' + instanceIp);

        try (FileOutputStream destinationOut = FileUtils.openOutputStream(new File(destinationPath + "/init.log"))) {
            LOGGER.info("[{}] destination path is exist, download can be started!", destinationPath);
            download(instanceIp, logPath, destinationPath);
        } catch (IOException e) {
            Log.error(LOGGER, format("Cannot download collected logs from '%s' at '%s' instance, because of: %s", logPath, instanceIp, e.getMessage()));
            throw new TestFailException(format("Cannot download collected logs from '%s' at '%s' instance!", logPath, instanceIp), e);
        }

        if (Files.exists(Path.of(String.join("/", workingDirectoryLocation, LOG_FOLDER_NAME)))) {
            LOGGER.info("[{}] logs successfully downloaded from instance [{}]", logPath, instanceIp);
        } else {
            LOGGER.info("[{}] logs could not be downloaded from instance [{}]", logPath, instanceIp);
        }
    }
}
