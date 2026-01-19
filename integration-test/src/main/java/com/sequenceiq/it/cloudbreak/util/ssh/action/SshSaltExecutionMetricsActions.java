package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static java.lang.String.format;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.salt.SaltFunctionReport;
import com.sequenceiq.it.cloudbreak.salt.SaltHighstateReport;
import com.sequenceiq.it.cloudbreak.salt.SaltStateReport;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshSaltExecutionMetricsActions {

    private static final String DEFAULT_TEST_METHOD_NAME = "unknown";

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSaltExecutionMetricsActions.class);

    @Value("${integrationtest.outputdir:.}")
    private String saltMetricsWorkingDirectory;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private SshJClient sshJClient;

    public List<SaltHighstateReport> getSaltExecutionMetrics(String environmentCrn, String resourceName, MicroserviceClient client, String serviceName,
            TestContext testContext) {
        return getSaltExecutionMetrics(environmentCrn, resourceName, client, saltMetricsWorkingDirectory, serviceName, testContext);
    }

    public List<SaltHighstateReport> getSaltExecutionMetrics(String environmentCrn, String resourceName, MicroserviceClient client,
            String workingDirectoryLocation, String serviceName, TestContext testContext) {
        String saltMasterIp = getSaltMasterIp(environmentCrn, resourceName, client, serviceName, testContext);
        List<SaltHighstateReport> saltHighstateReports = new ArrayList<>();
        try {
            if (!saltMasterIp.isBlank()) {
                String extractSaltMetricsCommand = getExtractSaltMetricsCommand(serviceName);
                Pair<Integer, String> cmdOut = sshJClientActions.executeSshCommand(saltMasterIp, extractSaltMetricsCommand);
                LOGGER.info("SSH test result on IP: [{}]: Return code: [{}], Result: {}", saltMasterIp, cmdOut.getLeft(), cmdOut.getRight());

                downloadSaltExecutionMetrics(saltMasterIp, workingDirectoryLocation, serviceName);
                unzipArchive(format("%s/salt_execution_metrics_%s.zip", workingDirectoryLocation, serviceName), new File(workingDirectoryLocation));
                saltHighstateReports = generateReport(workingDirectoryLocation, serviceName);
            } else {
                Log.warn(LOGGER, format(" Salt Master IP is missing! Salt execution metrics collection is not possible right now for resource: %s ",
                        resourceName));
            }
        } catch (Exception e) {
            Log.warn(LOGGER, format(" Error occurred while try to retrieve Salt execution metrics then generating reports! Instance: %s | Exception: %s ",
                    saltMasterIp, e.getMessage()));
        }
        return saltHighstateReports;
    }

    public void writeSaltStatesTotalDurationsReportsToFiles(TestContext testContext, String serviceName,
            Table<String, String, Double> saltStatesTotalDurations) {
        String fileName = String.format("salt_states_totalduration_report_%s_%s.log", serviceName,
                testContext.getTestMethodName().orElse(DEFAULT_TEST_METHOD_NAME));
        File outputDirectory = new File(saltMetricsWorkingDirectory);
        String filePath = outputDirectory.getPath() + FileSystems.getDefault().getSeparator() + fileName;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(saltStatesTotalDurations.toString());
            LOGGER.info("Log file for Salt States Totalduration report has been created successfully at: {}", filePath);
        } catch (IOException e) {
            LOGGER.error("Cannot create log file for Salt States Totalduration report!", e);
        }
    }

    private void downloadSaltExecutionMetrics(String instanceIp, String workingDirectoryLocation, String serviceName) throws IOException {
        SSHClient sshClient = sshJClient.createSshClient(instanceIp, null, null, null);
        sshClient.newSCPFileTransfer().download(format("/home/cloudbreak/salt_execution_metrics_%s.zip", serviceName), workingDirectoryLocation);

        if (Files.exists(Path.of(format("%s/salt_execution_metrics_%s.zip", workingDirectoryLocation, serviceName)))) {
            LOGGER.info("Salt execution metrics successfully downloaded from instance [{}]", instanceIp);
        } else {
            LOGGER.info("Salt execution metrics could not be downloaded from instance [{}]", instanceIp);
        }

        sshClient.close();
    }

    private String getExtractSaltMetricsCommand(String serviceName) {
        String commandResult = "";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("salt/export_salt_metrics.sh");

        if (inputStream == null) {
            throw new IllegalArgumentException("salt/export_salt_metrics.sh could not be found");
        }

        Scanner commands = new Scanner(inputStream).useDelimiter("\\A");
        while (commands.hasNext()) {
            commandResult = commands.next();
        }
        commands.close();
        return format(commandResult, serviceName, serviceName, serviceName, serviceName, serviceName, serviceName);
    }

    private String getSaltMasterIp(String environmentCrn, String resourceName, MicroserviceClient client, String serviceName, TestContext testContext) {
        switch (serviceName) {
            case "freeipa":
                return getFreeIpaGatewayPrivateIp(environmentCrn, (FreeIpaClient) client, testContext);
            case "sdx":
                List<InstanceGroupV4Response> sdxInstanceGroups = ((SdxClient) client).getDefaultClient(testContext).sdxEndpoint()
                        .getDetail(resourceName, Set.of()).getStackV4Response().getInstanceGroups();
                LOGGER.info("Sdx host groups found: {}", sdxInstanceGroups.toString());
                return getGatewayPrivateIp(sdxInstanceGroups);
            case "distrox":
                List<InstanceGroupV4Response> distroxInstanceGroups = ((CloudbreakClient) client).getDefaultClient(testContext).distroXV1Endpoint()
                        .getByName(resourceName, new HashSet<>()).getInstanceGroups();
                LOGGER.info("DistroX instance groups found: {}", distroxInstanceGroups.toString());
                return getGatewayPrivateIp(distroxInstanceGroups);
            default:
                return "";
        }
    }

    private String getFreeIpaGatewayPrivateIp(String environmentCrn, FreeIpaClient freeIpaClient, TestContext testContext) {
        return freeIpaClient.getDefaultClient(testContext).getFreeIpaV1Endpoint()
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

    private List<SaltHighstateReport> generateReport(String workingDirectoryLocation, String serviceName) {
        try {
            List<String> jids = Files.readAllLines(Path.of(format("%s/salt_jids_%s.txt", workingDirectoryLocation, serviceName)));
            List<SaltHighstateReport> saltHighstateReportList = new ArrayList<>();

            for (String jid : jids) {
                SaltHighstateReport saltHighstateReport = getHighstateReport(jid,
                        Path.of(format("%s/salt_job_result_%s.json", workingDirectoryLocation, jid)));
                saltHighstateReportList.add(saltHighstateReport);
            }

            return saltHighstateReportList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SaltHighstateReport getHighstateReport(String jid, Path jobResultPath) {
        try {
            String jsonString = Files.readString(jobResultPath);
            Map<String, Map<String, SaltFunctionReport>> map = new ObjectMapper().readValue(jsonString, new TypeReference<>() { });
            Map<String, List<SaltStateReport>> stateReportListForInstances = new HashMap<>();

            for (Entry<String, Map<String, SaltFunctionReport>> host : map.entrySet()) {
                List<SaltStateReport> saltStateReportList = new ArrayList<>();
                Map<String, List<Pair<String, SaltFunctionReport>>> methodsGroupedBySls = host.getValue().entrySet().stream()
                        .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                        .collect(Collectors.groupingBy(pair -> pair.getRight().getSls()));

                for (Entry<String, List<Pair<String, SaltFunctionReport>>> entry : methodsGroupedBySls.entrySet()) {
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
