package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.SSHClient;

@Component
public class SshCommandRunnerActions extends SshJClientActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshCommandRunnerActions.class);

    private String getGatewayPrivateIp(String environmentCrn, FreeIpaClient freeipaClient) {
        return freeipaClient.getDefaultClient().getFreeIpaV1Endpoint()
                .describe(environmentCrn).getInstanceGroups().stream()
                .filter(instanceGroup -> instanceGroup.getType().equals(InstanceGroupType.MASTER))
                .map(InstanceGroupResponse::getMetaData)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(instanceMetaData -> instanceMetaData.getInstanceType().equals(InstanceMetadataType.GATEWAY_PRIMARY))
                .map(InstanceMetaDataResponse::getPrivateIp)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new TestFailException(format("Cannot determine FreeIpa Gateway IP at environment: %s", environmentCrn)));
    }

    private String getGatewayPrivateIp(String clusterCrn, List<InstanceGroupV4Response> instanceGroups) {
        return instanceGroups.stream()
                .filter(instanceGroup -> instanceGroup.getType().equals(GATEWAY))
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(instanceMetaData -> instanceMetaData.getInstanceType().equals(GATEWAY_PRIMARY))
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new TestFailException(format("Cannot determine Gateway IP for cluster: %s", clusterCrn)));
    }

    private String getFileName(Map<String, String> fileNames, String key) {
        return fileNames.entrySet().stream()
                .filter(names -> names.getKey().equalsIgnoreCase(key))
                .map(Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private void upload(String instanceIp, String sourceFilePath, String destinationPath) {
        upload(instanceIp, null, null, null, sourceFilePath, destinationPath);
    }

    private void upload(String instanceIp, String user, String password, String privateKeyFilePath, String sourceFilePath, String destinationPath) {
        try (SSHClient sshClient = createSshClient(instanceIp, user, password, privateKeyFilePath)) {
            upload(sshClient, sourceFilePath, destinationPath);
            Log.log(LOGGER, format("File upload [%s] to host [%s] has been done.", sourceFilePath, instanceIp));
        } catch (Exception e) {
            Log.error(LOGGER, format("File upload [%s] to host [%s] is failing! %s", sourceFilePath, instanceIp, e.getMessage()));
            throw new TestFailException(format("File upload [%s] to host [%s] is failing!", sourceFilePath, instanceIp), e);
        }
    }

    private List<File> createCommandRunner() {
        List<String> commandRunnerFilePathes = List.of("commandrunner/qa-command-runner.py", "commandrunner/sample-command.json");
        List<File> createdTmpFiles = new ArrayList<>();

        commandRunnerFilePathes.forEach(filePath -> {
            File tmpFile;
            URL url = Thread.currentThread().getContextClassLoader().getResource(filePath);
            if (url != null) {
                String fileName = url.getFile();
                InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
                try {
                    String name = Arrays.stream(StringUtils.substringsBetween(fileName, "/", ".")).reduce((first, last) -> last).get();
                    String extension = StringUtils.substringAfterLast(fileName, ".");
                    LOGGER.info("Creating tmp file as: {}.{}...", name, extension);
                    tmpFile = File.createTempFile(name, '.' + extension);
                    FileUtils.copyInputStreamToFile(inputStream, tmpFile);
                    String tmpFileContent = Files.readString(Path.of(tmpFile.getPath()).normalize().toAbsolutePath());
                    LOGGER.info(format("Temporary file has been created at '%s' path with content:%n%s!", tmpFile.getAbsolutePath(), tmpFileContent));
                    createdTmpFiles.add(tmpFile);
                } catch (IOException e) {
                    Log.error(LOGGER, format("'%s' file cannot be created for tests, because of: %s", filePath, e.getMessage()));
                    throw new TestFailException(format("'%s' file cannot be created for tests!", filePath), e);
                }
            } else {
                Log.error(LOGGER, format("Cannot find file at path: '%s'!", filePath));
                throw new TestFailException(format("Cannot find file at path: '%s'!", filePath));
            }
        });
        return createdTmpFiles;
    }

    private Map<String, String> uploadCommandRunner(String gatewayPrivateIp) {
        Map<String, String> fileNames = new HashMap<>();

        if (StringUtils.isBlank(gatewayPrivateIp) || StringUtils.containsIgnoreCase(gatewayPrivateIp, "N/A")) {
            Log.error(LOGGER, "FreeIPA GATEWAY Private IP is not available!");
            throw new TestFailException("FreeIPA GATEWAY Private IP is not available!");
        }

        createCommandRunner().forEach(file -> {
            if (file.exists()) {
                String tmpPath = Path.of(file.getPath()).normalize().toAbsolutePath().toString();
                String destPath = "/home/cloudbreak/";
                LOGGER.info("Uploading file from path '{}' to host '{}' and path '{}'...", tmpPath, gatewayPrivateIp, destPath);
                upload(gatewayPrivateIp, tmpPath, destPath);
                if (file.getName().contains("command-runner")) {
                    fileNames.put("runner", file.getName());
                } else {
                    fileNames.put("command", file.getName());
                }
            } else {
                Log.error(LOGGER, "Cannot find command runner folder at classpath!");
                throw new TestFailException("Cannot find command runner folder at classpath!");
            }
        });

        return fileNames;
    }

    private void executeCommandRunner(String gatewayPrivateIp, List<String> commandNames) {
        Map<String, String> fileNames = uploadCommandRunner(gatewayPrivateIp);
        Pair<Integer, String> cmdOut;
        String commandRunnerFileName = getFileName(fileNames, "runner");
        String sampleCommandFileName = getFileName(fileNames, "command");

        if (CollectionUtils.isEmpty(commandNames)) {
            cmdOut = executeSshCommand(gatewayPrivateIp, format("sudo python3 /srv/salt/qa/%s -c /srv/salt/qa/%s",
                    commandRunnerFileName, sampleCommandFileName));
        } else {
            cmdOut = executeSshCommand(gatewayPrivateIp, format("sudo python3 /srv/salt/qa/%s -c /srv/salt/qa/%s -r %s",
                    commandRunnerFileName, sampleCommandFileName, commandNames));
        }

        try {
            JSONObject cmdOutJson = new JSONObject(cmdOut.getValue());
            int code = cmdOutJson.getInt("code");
            if (code != 0) {
                String error = cmdOutJson.getString("err");
                Log.error(LOGGER, "One or more command has been failed with error: %s", error);
                throw new TestFailException(format("One or more command has been failed with error: %s", error));
            }
        } catch (JSONException e) {
            Log.error(LOGGER, "Cannot parse result to JSON!");
            throw new TestFailException("Cannot parse result to JSON!");
        }
    }

    public FreeIpaTestDto executeCommandRunner(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient, List<String> commandNames) {
        String gatewayPrivateIp = getGatewayPrivateIp(environmentCrn, freeipaClient);
        executeCommandRunner(gatewayPrivateIp, commandNames);
        return testDto;
    }

    public <T extends CloudbreakTestDto> T executeCommandRunner(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> commandNames) {
        String gatewayPrivateIp = getGatewayPrivateIp(testDto.getCrn(), instanceGroups);
        executeCommandRunner(gatewayPrivateIp, commandNames);
        return testDto;
    }
}
