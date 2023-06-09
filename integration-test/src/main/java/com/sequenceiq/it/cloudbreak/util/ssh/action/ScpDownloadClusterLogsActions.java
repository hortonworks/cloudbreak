package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.String.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.SSHClient;

@Component
public class ScpDownloadClusterLogsActions extends SshJClientActions {

    private static final String DEFAULT_TEST_METHOD_NAME = "unknown";

    private static final String LOG_FOLDER_NAME = "yarn_cluster_logs";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScpDownloadClusterLogsActions.class);

    @Value("${integrationtest.outputdir:.}")
    private String workingDirectory;

    public void downloadClusterLogs(String environmentCrn, String resourceName, String masterIp, String serviceName) {
        downloadClusterLogs(environmentCrn, resourceName, masterIp, workingDirectory, serviceName);
    }

    public void downloadClusterLogs(String environmentCrn, String resourceName, String masterIp,
            String workingDirectoryLocation, String serviceName) {
        try {
            if (!masterIp.isBlank()) {
                copyLogsToCloudbreak(masterIp, "/var/log/", "var-logs");
                downloadLogs(masterIp, workingDirectoryLocation, serviceName, "var-logs.tar.gz");
                if (!StringUtils.equalsIgnoreCase("freeipa", serviceName)) {
                    copyLogsToCloudbreak(masterIp, "/var/run/cloudera-scm-agent/process/", "cm-logs");
                    downloadLogs(masterIp, workingDirectoryLocation, serviceName, "cm-logs.tar.gz");
                }
            } else {
                Log.warn(LOGGER, format(" Master IP is missing! Cluster log collection is not possible right now for resource: %s ",
                        resourceName));
            }
        } catch (Exception e) {
            Log.warn(LOGGER, format(" Error occurred while try to retrieve cluster logs! Instance: %s | Exception: %s ",
                    masterIp, e.getMessage()));
        }
    }

    private void copyLogsToCloudbreak(String instanceIp, String sourceFilePath, String destinationDirectory) {
        String mkdirLogsDirectoryCommand = format("mkdir -p %s", destinationDirectory);
        executeSshCommand(instanceIp, mkdirLogsDirectoryCommand);
        executeSshCommand(instanceIp, format("sudo cp -ru %s %s/ && sudo chown -R cloudbreak:cloudbreak %s", sourceFilePath, destinationDirectory,
                destinationDirectory));
        executeSshCommand(instanceIp, format("tar -czf %s.tar.gz %s && rm -rf %s", destinationDirectory, destinationDirectory, destinationDirectory));
    }

    private void download(String instanceIp, String sourceFilePath, String destinationPath) {
        download(instanceIp, null, null, null, sourceFilePath, destinationPath);
    }

    private void download(String instanceIp, String user, String password, String privateKeyFilePath, String sourceFilePath, String destinationPath) {
        try (SSHClient sshClient = createSshClient(instanceIp, user, password, privateKeyFilePath)) {
            download(sshClient, sourceFilePath, destinationPath);
            Log.log(LOGGER, format("File download [%s] from host [%s] has been done.", sourceFilePath, instanceIp));
        } catch (Exception e) {
            Log.error(LOGGER, format("File download [%s] from host [%s] is failing! %s", sourceFilePath, instanceIp, e.getMessage()));
            throw new TestFailException(format("File download [%s] from host [%s] is failing!", sourceFilePath, instanceIp), e);
        }
    }

    private void downloadLogs(String instanceIp, String workingDirectoryLocation, String serviceName, String logPath) {
        String destinationPath = String.join("/", workingDirectoryLocation, LOG_FOLDER_NAME, serviceName);

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
