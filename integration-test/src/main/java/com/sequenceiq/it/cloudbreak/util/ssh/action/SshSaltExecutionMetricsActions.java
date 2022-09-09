package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.EnvironmentAware;
import com.sequenceiq.it.cloudbreak.salt.SaltFunctionReport;
import com.sequenceiq.it.cloudbreak.salt.SaltHighstateReport;
import com.sequenceiq.it.cloudbreak.salt.SaltStateReport;

import net.schmizz.sshj.SSHClient;

@Component
public class SshSaltExecutionMetricsActions extends SshJClientActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSaltExecutionMetricsActions.class);

    public CloudbreakTestDto getSaltExecutionMetrics(TestContext testContext, CloudbreakTestDto testDto, MicroserviceClient client,
            String workingDirectoryLocation, String serviceName) {

        String saltMasterIp = getSaltMasterIp(testDto, client, serviceName);
        if (!saltMasterIp.isBlank()) {
            try {
                String extractSaltMetricsCommand = getExtractSaltMetricsCommand(serviceName);
                Pair<Integer, String> cmdOut = executeSshCommand(saltMasterIp, extractSaltMetricsCommand);
                LOGGER.info("SSH test result on IP: [{}]: Return code: [{}], Result: {}", saltMasterIp, cmdOut.getLeft(), cmdOut.getRight());

                downloadSaltExecutionMetrics(saltMasterIp, workingDirectoryLocation, serviceName);
                unzipArchive(workingDirectoryLocation + "/salt_execution_metrics_" + serviceName + ".zip", new File(workingDirectoryLocation));
                generateReport(workingDirectoryLocation, serviceName, testContext.getTestMethodName().orElse("unknown"));
            } catch (IOException e) {
                LOGGER.info("Error occurred while trying to retrieve Salt execution metrics and generating report on instance [{}]: {}",
                        saltMasterIp, e.getMessage());
            }

            return testDto;
        } else {
            throw new RuntimeException(String.format("Couldn't collect salt execution metrics for %s", testDto.getName()));
        }
    }

    private void downloadSaltExecutionMetrics(String instanceIp, String workingDirectoryLocation, String serviceName) throws IOException {
        SSHClient sshClient = createSshClient(instanceIp, null, null, null);
        sshClient.newSCPFileTransfer().download("/home/cloudbreak/salt_execution_metrics_" + serviceName + ".zip", workingDirectoryLocation);

        if (Files.exists(Path.of(workingDirectoryLocation + "/salt_execution_metrics_" + serviceName + ".zip"))) {
            LOGGER.info("Salt execution metrics successfully downloaded from instance [{}]", instanceIp);
        } else {
            LOGGER.info("Salt execution metrics could not be downloaded from instance [{}]", instanceIp);
        }

        sshClient.close();
    }

    private String getExtractSaltMetricsCommand(String serviceName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("salt/export_salt_metrics.sh");

        if (inputStream == null) {
            throw new IllegalArgumentException("salt/export_salt_metrics.sh could not be found");
        }

        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        return String.format(result, serviceName, serviceName, serviceName, serviceName, serviceName, serviceName);
    }

    private String getSaltMasterIp(CloudbreakTestDto testDto, MicroserviceClient client, String serviceName) {
        switch (serviceName) {
            case "freeipa":
                return getFreeIpaGatewayPrivateIp(((EnvironmentAware) testDto).getEnvironmentCrn(), (FreeIpaClient) client);
            case "sdx":
                List<InstanceGroupV4Response> sdxInstanceGroups = ((SdxClient) client).getDefaultClient().sdxEndpoint()
                        .getDetail(testDto.getName(), Set.of()).getStackV4Response().getInstanceGroups();
                LOGGER.info("Sdx host groups found: {}", sdxInstanceGroups.toString());

                return getGatewayPrivateIp(sdxInstanceGroups);
            case "distrox":
                List<InstanceGroupV4Response> distroxInstanceGroups = ((CloudbreakClient) client).getDefaultClient().distroXV1Endpoint()
                        .getByName(testDto.getName(), new HashSet<>()).getInstanceGroups();
                LOGGER.info("DistroX instance groups found: {}", distroxInstanceGroups.toString());

                return getGatewayPrivateIp(distroxInstanceGroups);
            default:
                return "";
        }
    }

    private String getFreeIpaGatewayPrivateIp(String environmentCrn, FreeIpaClient freeIpaClient) {
        return freeIpaClient.getDefaultClient().getFreeIpaV1Endpoint()
                .describe(environmentCrn).getInstanceGroups().stream()
                .filter(instanceGroup -> instanceGroup.getType().equals(InstanceGroupType.MASTER))
                .map(InstanceGroupResponse::getMetaData)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(instanceMetaData -> instanceMetaData.getInstanceType().equals(InstanceMetadataType.GATEWAY_PRIMARY))
                .map(InstanceMetaDataResponse::getPrivateIp)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String getGatewayPrivateIp(List<InstanceGroupV4Response> instanceGroups) {
        return instanceGroups.stream()
                .filter(instanceGroup -> instanceGroup.getType().equals(GATEWAY))
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(instanceMetaData -> instanceMetaData.getInstanceType().equals(GATEWAY_PRIMARY))
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void unzipArchive(String archive, File destinationDirectory) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                File newFile = newFile(destinationDirectory, zipEntry);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private void generateReport(String workingDirectoryLocation, String serviceName, String testName) {
        try {
            List<String> jids = Files.readAllLines(Path.of(workingDirectoryLocation + "/salt_jids_" + serviceName + ".txt"));
            List<SaltHighstateReport> saltHighstateReportList = new ArrayList<>();

            for (String jid : jids) {
                SaltHighstateReport saltHighstateReport = getHighstateReport(jid, Path.of(workingDirectoryLocation + "/salt_job_result_" + jid + ".json"));
                saltHighstateReportList.add(saltHighstateReport);
            }

            new ObjectMapper().writeValue(
                    new File(workingDirectoryLocation + "/salt_metrics_report_" + serviceName + "_" + testName + ".json"), saltHighstateReportList);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SaltHighstateReport getHighstateReport(String jid, Path jobResultPath) {
        try {
            String jsonString = Files.readString(jobResultPath);
            Map<String, Map<String, SaltFunctionReport>> map = new ObjectMapper().readValue(jsonString, new TypeReference<>() {
            });

            Map<String, List<SaltStateReport>> stateReportListForInstances = new HashMap<>();

            for (Map.Entry<String, Map<String, SaltFunctionReport>> host : map.entrySet()) {
                List<SaltStateReport> saltStateReportList = new ArrayList<>();
                Map<String, List<Pair<String, SaltFunctionReport>>> methodsGroupedBySls = host.getValue().entrySet().stream()
                        .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                        .collect(Collectors.groupingBy(pair -> pair.getRight().getSls()));

                for (Map.Entry<String, List<Pair<String, SaltFunctionReport>>> entry : methodsGroupedBySls.entrySet()) {
                    saltStateReportList.add(new SaltStateReport(entry.getKey(),
                            entry.getValue().stream()
                                    .sorted((a, b) -> Double.compare(b.getRight().getDuration(), a.getRight().getDuration()))
                                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a, b) -> a, LinkedHashMap::new)),
                            entry.getValue().stream()
                                    .reduce(0.0, (sum, pair) -> sum + pair.getRight().getDuration(), Double::sum)));
                }

                stateReportListForInstances.put(host.getKey(), saltStateReportList);
            }

            return new SaltHighstateReport(jid, stateReportListForInstances);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
