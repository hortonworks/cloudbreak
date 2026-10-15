package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil.getAllInstanceIps;
import static com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil.getFreeIpaMasterInstanceIps;
import static com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil.getInstanceGroupIps;
import static com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil.getPrimaryGatewayIps;
import static java.lang.String.format;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.assertion.encryptionprofile.EncryptionProfileAssertions;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClientFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.UserAuthException;

@Component
public class SshJClientActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientActions.class);

    private static final Set<String> AZURE_SKIPPED_FREEIPA_LOGGING_AGENT_STATUSES = Set.of("s3Accessible", "databusS3Accessible");

    @Inject
    private SshJClientFactory sshJClientFactory;

    @Inject
    private SshJClient sshJClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private EncryptionProfileAssertions encryptionProfileAssertions;

    private List<String> getFreeIpaInstanceGroupIps(InstanceMetadataType istanceMetadataType, String environmentCrn, FreeIpaClient freeipaClient,
            boolean publicIp, TestContext testContext) {
        return getFreeIpaMasterInstanceIps(freeipaClient.getDefaultClient(testContext).getFreeIpaV1Endpoint()
                .describe(environmentCrn).getInstanceGroups(), istanceMetadataType, publicIp);
    }

    private void validateRequiredNumberOfFiles(Map<String, Long> filesByIp, String filePath, long requiredNumberOfFiles) {
        filesByIp.forEach((ip, fileCount) -> {
            if (requiredNumberOfFiles == fileCount) {
                Log.log(LOGGER, " Required number (%d) of files are available at '%s' on '%s' instance. ", fileCount, filePath, ip);
            } else {
                LOGGER.error("Required number ({}) of files are NOT available at '{}' on '{}' instance!", requiredNumberOfFiles, filePath, ip);
                throw new TestFailException(format("Required number (%d) of files are NOT available at '%s' on '%s' instance!", requiredNumberOfFiles,
                        filePath, ip));
            }
        });
    }

    public <T extends AbstractSdxTestDto> T checkFilesByNameAndPath(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            String filePath, String fileName, long requiredNumberOfFiles, String user, String password) {
        String fileListCommand = format("find %s -type f -name %s", filePath, fileName);
        Map<String, Long> filesByIp = getInstanceGroupIps(instanceGroups, hostGroupNames, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executefileListCommand(ip, user, password, fileListCommand)));

        validateRequiredNumberOfFiles(filesByIp, filePath, requiredNumberOfFiles);
        return testDto;
    }

    public <T extends AbstractFreeIpaTestDto> T checkFilesByNameAndPath(T testDto, String environmentCrn, FreeIpaClient freeipaClient,
            InstanceMetadataType istanceMetadataType, String filePath, String fileName, long requiredNumberOfFiles, String user, String password) {
        String fileListCommand = format("find %s -type f -name %s", filePath, fileName);
        Map<String, Long> filesByIp = getFreeIpaInstanceGroupIps(istanceMetadataType, environmentCrn, freeipaClient, false, testDto.getTestContext())
                .stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executefileListCommand(ip, user, password, fileListCommand)));

        validateRequiredNumberOfFiles(filesByIp, filePath, requiredNumberOfFiles);
        return testDto;
    }

    public void checkAwsEphemeralDisksMounted(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, String mountDirPrefix) {
        Map<String, SshCommandOutcome> deviceMountPointMappingsByIp =
                getDeviceMountPointMappings(instanceGroups, hostGroupNames, "hadoopfs").toMapByInstanceIp();

        for (SshCommandOutcome node : getDeviceDiskTypeMappings(instanceGroups, hostGroupNames)) {
            Map<String, String> ephemeralDisks = new Json(node.commandOutput()).getMap().entrySet().stream()
                    .filter(e -> String.valueOf(e.getValue()).contains("Amazon EC2 NVMe Instance Storage"))
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            if (ephemeralDisks.isEmpty()) {
                LOGGER.error("Instance store volume missing from node with IP {}!", node.instanceIp());
                throw new TestFailException(format("Instance store volume missing from node with IP %s!", node.instanceIp()));
            }

            if (deviceMountPointMappingsByIp.get(node.instanceIp()) == null) {
                LOGGER.error("No device mount point mappings found for node with IP {}!", node.instanceIp());
                throw new TestFailException(format("No device mount point mappings found for node with IP %s!", node.instanceIp()));
            }
            Map<String, String> mountPoints = new Json(deviceMountPointMappingsByIp.get(node.instanceIp()).commandOutput()).getMap().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            for (String device : ephemeralDisks.keySet()) {
                String mountPoint = mountPoints.get(device);
                if (mountPoint == null) {
                    LOGGER.error("No mount point found for ephemeral device {} on node with IP {}!", device, node.instanceIp());
                    throw new TestFailException(format("No mount point found for device %s on node with IP %s!", device, node.instanceIp()));
                } else if (!mountPoint.contains(mountDirPrefix)) {
                    LOGGER.error("Ephemeral device {} incorrectly mounted to {} on node with IP {}!", device, mountPoint, node.instanceIp());
                    throw new TestFailException(format("Ephemeral device %s incorrectly mounted to %s on node with IP %s!",
                            device, mountPoint, node.instanceIp()));
                }
            }
        }
    }

    public Set<String> getAwsEphemeralVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        Map<String, SshCommandOutcome> deviceMountPointMappingsByIp =
                getDeviceMountPointMappings(instanceGroups, hostGroupNames, "hadoopfs").toMapByInstanceIp();
        SshCommandOutcomes deviceDiskTypeMappings = getDeviceDiskTypeMappings(instanceGroups, hostGroupNames);

        Map<String, String> mountPoints = deviceDiskTypeMappings.stream().findFirst()
                .stream()
                .map(node -> new Json(deviceMountPointMappingsByIp.get(node.instanceIp()).commandOutput()).getMap().entrySet())
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

        return deviceDiskTypeMappings.stream().findFirst()
                .stream()
                .map(node -> new Json(node.commandOutput()).getMap().entrySet().stream()
                        .filter(e -> String.valueOf(e.getValue()).contains("Amazon EC2 NVMe Instance Storage"))
                        .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue()))))
                .map(Map::keySet)
                .flatMap(Set::stream)
                .map(mountPoints::get)
                .collect(Collectors.toSet());
    }

    public void checkAzureTemporalDisksMounted(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, String mountDir) {
        getDeviceMountPointMappings(instanceGroups, hostGroupNames, mountDir).forEach(outcome -> {
            if (StringUtils.isBlank(outcome.commandOutput()) || !outcome.returnCodeSuccess()) {
                LOGGER.error("No mount point found '{}' on node with IP {}!", mountDir, outcome.instanceIp());
                throw new TestFailException(format("No mount point found '%s' on node with IP %s!", mountDir, outcome.instanceIp()));
            } else if (!StringUtils.containsIgnoreCase(outcome.commandOutput(), mountDir)) {
                LOGGER.error("Device incorrectly mounted to '{}' on node with IP {}!", mountDir, outcome.instanceIp());
                throw new TestFailException(format("Device incorrectly mounted to '%s' on node with IP %s!", mountDir, outcome.instanceIp()));
            }
        });
    }

    private SshCommandOutcomes getDeviceMountPointMappings(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            String mountDir) {
        String diskMountPointListCmd =
                "df | grep " + mountDir + " | awk '{print $1,$6}' | " +
                        "sed -e \"s/\\(.*\\) \\(.*\\)/\\\"\\1\\\":\\\"\\2\\\"/\" | paste -s -d ',' | sed 's/.*/{\\0}/'";
        return executeOnIps(getInstanceGroupIps(instanceGroups, hostGroupNames, false), diskMountPointListCmd).requireAllExecuted();
    }

    private SshCommandOutcomes getDeviceDiskTypeMappings(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        String diskTypeListCmd =
                "sudo nvme list -o json | jq -cM '[.Devices[] | {(.DevicePath): .ModelNumber}] | add // {}'";
        return executeOnIps(getInstanceGroupIps(instanceGroups, hostGroupNames, false), diskTypeListCmd).requireAllExecuted();
    }

    public SshCommandOutcomes executeSshCommandOnHost(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            String sshCommand, boolean publicIp) {
        return executeOnIps(getInstanceGroupIps(instanceGroups, hostGroupNames, publicIp), sshCommand);
    }

    public SshCommandOutcomes executeSshCommandOnHost(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            String user, String password, String privateKeyFilePath, String sshCommand, boolean publicIp) {
        return executeOnIps(getInstanceGroupIps(instanceGroups, hostGroupNames, publicIp), user, password, privateKeyFilePath, sshCommand);
    }

    public SshCommandOutcomes executeSshCommandOnAllHosts(Collection<InstanceGroupV4Response> instanceGroups,
            String sshCommand, boolean publicIp, String privateKeyFilePath) {
        return executeOnIps(getAllInstanceIps(instanceGroups, publicIp), "cloudbreak", null, privateKeyFilePath, sshCommand);
    }

    public SshCommandOutcomes executeSshCommandOnHosts(Collection<String> ipAddresses, String sshCommand) {
        return executeOnIps(ipAddresses, sshCommand);
    }

    public SshCommandOutcomes executeSshCommandOnPrimaryGateways(Collection<InstanceGroupV4Response> instanceGroups, String sshCommand,
            boolean publicIp) {
        return executeOnIps(getPrimaryGatewayIps(instanceGroups, publicIp), sshCommand);
    }

    public SdxTestDto checkNoOutboundInternetTraffic(SdxTestDto testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        getInstanceGroupIps(instanceGroups, hostGroupNames, true).forEach(this::checkNoOutboundInternetTraffic);
        return testDto;
    }

    public FreeIpaTestDto checkNoOutboundInternetTraffic(FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, testDto.getResponse().getEnvironmentCrn(), freeIpaClient, true,
                testDto.getTestContext())
                .forEach(this::checkNoOutboundInternetTraffic);
        return testDto;
    }

    private void checkNoOutboundInternetTraffic(String instanceIp) {
        SshCommandOutcome cmdOut = executeSshCommand(instanceIp, "curl --max-time 30 cloudera.com").requireExecuted();
        if (cmdOut.returnCodeSuccess()) {
            throw new TestFailException("Instance [" + instanceIp + "] has internet connection but shouldn't have!");
        }
    }

    public SdxTestDto checkKinitDuringFreeipaUpgrade(SdxTestDto testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        getInstanceGroupIps(instanceGroups, hostGroupNames, false).stream().findFirst()
                .ifPresentOrElse(this::checkKinitDuringFreeipaUpgrade, () -> {
                    throw new TestFailException("No instance found for kinit test");
                });
        return testDto;
    }

    private void checkKinitDuringFreeipaUpgrade(String instanceIp) {
        // TODO remove sleep 31 after unbound removal has been done, this is because unbound cached for 30 sec the dns records
        executeSshCommand(instanceIp,
                "set -x kdestroy && echo Password123! | KRB5_TRACE=/dev/stdout kinit -V fakemockuser0 " +
                        "|| sleep 31 && kdestroy && echo Password123! | KRB5_TRACE=/dev/stdout kinit -V fakemockuser0 && klist | grep fakemockuser0")
                .requireSuccess("Kinit during freeipa upgrade");
    }

    /**
     * Executes the command on the instance. SSH connection and authentication failures are returned as an {@link SshCommandOutcome} instead
     * of being thrown, so the caller decides how to handle them, e.g. with {@link SshCommandOutcome#requireSuccess(String)}.
     */
    public SshCommandOutcome executeCommand(String instanceIp, String user, String password, String privateKeyFilePath, String command) {
        if (StringUtils.isBlank(instanceIp)) {
            throw new TestFailException(format("SSH command [%s] cannot be executed, because the instance IP is blank", command));
        }
        try (SSHClient sshClient = sshJClientFactory.createSshClient(instanceIp, user, password, privateKeyFilePath)) {
            try {
                SshCommandOutcome outcome = sshJClient.execute(sshClient, instanceIp, command);
                Log.log(LOGGER, " Command exit status '%s' and result '%s'. ", outcome.returnCode(), outcome.commandOutput());
                return outcome;
            } catch (SocketTimeoutException e) {
                LOGGER.warn("SSH command [{}] timed out on [{}]. {}", command, instanceIp, e.getMessage());
                return SshCommandOutcome.timedOut(instanceIp, e);
            }
        } catch (UserAuthException e) {
            LOGGER.warn("SSH authentication failed on [{}]. {}", instanceIp, e.getMessage());
            return SshCommandOutcome.authFailure(instanceIp, e);
        } catch (IOException e) {
            LOGGER.warn("SSH interaction failed on [{}] while executing command [{}], treating host as unreachable. {}",
                    instanceIp, command, e.getMessage());
            return SshCommandOutcome.unreachable(instanceIp, e);
        }
    }

    public SshCommandOutcome executeSshCommand(String instanceIp, String command) {
        return executeCommand(instanceIp, null, null, null, command);
    }

    private SshCommandOutcomes executeOnIps(Collection<String> instanceIps, String command) {
        return executeOnIps(instanceIps, null, null, null, command);
    }

    private SshCommandOutcomes executeOnIps(Collection<String> instanceIps, String user, String password, String privateKeyFilePath, String command) {
        return new SshCommandOutcomes(instanceIps.stream()
                .distinct()
                .map(ip -> executeCommand(ip, user, password, privateKeyFilePath, command))
                .toList());
    }

    private long executefileListCommand(String instanceIP, String user, String password, String fileListCommand) {
        SshCommandOutcome cmdOut = executeCommand(instanceIP, user, password, null, fileListCommand).requireExecuted("Listing files");

        List<String> foundFilePaths = Stream.of(cmdOut.commandOutput().split("[\\r\\n\\t]"))
                .filter(outputValue -> outputValue.strip().startsWith("/"))
                .toList();
        Log.log(LOGGER, format(" The file is present '%s' at '%s' path. ", !foundFilePaths.isEmpty(), foundFilePaths.stream().findFirst().orElse(null)));
        return foundFilePaths.size();
    }

    private <T extends CloudbreakTestDto> T eventStatusesNotOkValidation(T testDto, SshCommandOutcomes statusReports,
            String acceptableNokEventName) {
        for (SshCommandOutcome statusReport : statusReports) {
            List<String> statusesNotOk = new Json(statusReport.commandOutput()).getMap().entrySet().stream()
                    .filter(status -> String.valueOf(status.getValue()).contains("NOK"))
                    .map(Entry::getKey).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(statusesNotOk)) {
                statusesNotOk.forEach(event -> {
                    if (StringUtils.isNotBlank(acceptableNokEventName) && StringUtils.containsIgnoreCase(event, acceptableNokEventName)) {
                        Log.log(LOGGER, format(" Found '%s' status is not OK at '%s' instance. However this is acceptable!", acceptableNokEventName,
                                statusReport.instanceIp()));
                    } else {
                        Log.error(LOGGER, format(" There is 'Not OK' status %s is present on '%s' instance! ", event,
                                statusReport.instanceIp()));
                        throw new TestFailException(format("There is 'Not OK' status %s is present on '%s' instance!", event,
                                statusReport.instanceIp()));
                    }
                });
            }
        }
        return testDto;
    }

    public <T extends CloudbreakTestDto> T checkMonitoringStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            List<String> verifyMetricNames, List<String> acceptableNokNames) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return checkMonitoringStatus(testDto, instanceIps, verifyMetricNames, acceptableNokNames);
    }

    public FreeIpaTestDto checkMonitoringStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient,
            List<String> verifyMetricNames, List<String> acceptableNokNames) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                testDto.getTestContext());
        return checkMonitoringStatus(testDto, instanceIps, verifyMetricNames, acceptableNokNames);
    }

    public <T extends CloudbreakTestDto> T checkMonitoringStatus(T testDto, List<String> instanceIps, List<String> verifyMetricNames,
            List<String> acceptableNokNames) {

        String monitoringStatusCommand = format("sudo cdp-doctor monitoring status -m %s --format json", StringUtils.join(verifyMetricNames, ","));
        for (SshCommandOutcome monitoringStatusReport : executeOnIps(instanceIps, monitoringStatusCommand).requireAllExecuted()) {
            try {
                List<String> statusCategories = List.of("services", "scrapping", "metrics");
                Map<String, Map<String, String>> fetchedMonitoringStatus = JsonUtil.readValue(monitoringStatusReport.commandOutput(),
                        new TypeReference<Map<String, Map<String, String>>>() {
                        });
                statusCategories.forEach(statusCategory -> {
                    Map<String, String> statusesNotOkInCategory = fetchedMonitoringStatus.entrySet().stream()
                            .filter(categories -> statusCategory.equalsIgnoreCase(categories.getKey()))
                            .flatMap(selectedCategory -> selectedCategory.getValue().entrySet().stream()
                                    .filter(servicesInCategory -> "NOK".equalsIgnoreCase(servicesInCategory.getValue())))
                            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                    if (MapUtils.isNotEmpty(statusesNotOkInCategory)) {
                        statusesNotOkInCategory.forEach((service, status) -> {
                            if (acceptableNokNames.stream()
                                    .anyMatch(nokAccepted -> nokAccepted.equalsIgnoreCase(service))) {
                                Log.log(LOGGER, format(" Found Monitoring '%s' where %s' is 'Not OK' at '%s' instance. " +
                                        "However this is acceptable! ", statusCategory.toUpperCase(Locale.ROOT), service, monitoringStatusReport.instanceIp()));
                            } else {
                                Log.error(LOGGER, format(" Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance! ",
                                        statusCategory.toUpperCase(Locale.ROOT), service, monitoringStatusReport.instanceIp()));
                                throw new TestFailException(format("Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance!",
                                        statusCategory.toUpperCase(Locale.ROOT), service, monitoringStatusReport.instanceIp()));
                            }
                        });
                    }
                });
            } catch (IOException | IllegalStateException e) {
                Log.error(LOGGER, " Cannot parse Common Monitoring Status Report JSON! ");
                throw new TestFailException("Cannot parse Common Monitoring Status Report JSON: ", e);
            }
        }
        return testDto;
    }

    private void createHugheFilesForFilesystemFreeBytesMetric(List<String> instanceIps) {
        String listDisk = "sudo df -h /";
        String allocateBigFiles = "sudo seq -f %04.0f 4 | xargs -I '{}' fallocate -l 5G test_file'{}'.txt";

        executeOnIps(instanceIps, StringUtils.join(List.of(listDisk, allocateBigFiles, listDisk), " && ")).requireAllExecuted();

        waitingFor(Duration.ofMinutes(1), "Waiting for Monitoring 'node_filesystem_free_bytes' metric to be generated has been interrupted");
    }

    private void removeHugheFilesForFilesystemFreeBytesMetric(List<String> instanceIps) {
        String removeAllocatedFiles = "sudo rm -rf test_file{0001..0004}.txt";
        String listDisk = "sudo df -h /";

        executeOnIps(instanceIps, StringUtils.join(List.of(listDisk, removeAllocatedFiles, listDisk), " && ")).requireAllExecuted();

        waitingFor(Duration.ofMinutes(1), "Waiting for Monitoring 'node_filesystem_free_bytes' metric to be generated has been interrupted");
    }

    private void waitingFor(Duration duration, String interruptedMessage) {
        Duration waitDuration = duration == null ? Duration.ofMinutes(0) : duration;
        String intrMessage = interruptedMessage == null ? "Waiting has been interrupted:" : interruptedMessage;
        try {
            Thread.sleep(waitDuration.toMillis());
            LOGGER.info("Wait '{}' duration has been done.", duration.toString());
        } catch (InterruptedException e) {
            LOGGER.warn(StringUtils.join(intrMessage, e));
        }
    }

    public <T extends CloudbreakTestDto> T checkFilesystemFreeBytesGeneratedMetric(T testDto, List<InstanceGroupV4Response> instanceGroups,
            List<String> hostGroupNames) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return checkFilesystemFreeBytesGeneratedMetric(testDto, instanceIps);
    }

    public FreeIpaTestDto checkFilesystemFreeBytesGeneratedMetric(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                testDto.getTestContext());
        return checkFilesystemFreeBytesGeneratedMetric(testDto, instanceIps);
    }

    private List<String> checkRequestSignerSuccessCount(List<String> instanceIps) {
        List<String> requestSignerSuccessCount = new ArrayList<>();
        String monitoringStatusCommand = "sudo cdp-doctor monitoring status -m node_filesystem_free_bytes --format json";
        for (SshCommandOutcome monitoringStatusReport : executeOnIps(instanceIps, monitoringStatusCommand).requireAllExecuted()) {
            try {
                String requestSignerCategory = "request-signer";
                Map<String, Map<String, String>> fetchedMonitoringStatus = JsonUtil.readValue(monitoringStatusReport.commandOutput(),
                        new TypeReference<Map<String, Map<String, String>>>() {
                        });
                List<String> successCount = fetchedMonitoringStatus.entrySet().stream()
                        .filter(categories -> requestSignerCategory.equalsIgnoreCase(categories.getKey()))
                        .flatMap(selectedCategory -> selectedCategory.getValue().entrySet().stream()
                                .filter(servicesInCategory -> "success_count".equalsIgnoreCase(servicesInCategory.getKey())))
                        .map(Entry::getValue)
                        .toList();
                if (!successCount.isEmpty()) {
                    Log.log(LOGGER, format(" Monitoring Request Signer success count is '%s' at '%s' instance. ", successCount,
                            monitoringStatusReport.instanceIp()));
                    successCount.forEach(count -> {
                        if (Integer.parseInt(count) > 0) {
                            Log.log(LOGGER, format(" Found Monitoring Request Signer success count is '%s' at '%s' instance! ", count,
                                    monitoringStatusReport.instanceIp()));
                            requestSignerSuccessCount.add(count);
                        } else {
                            Log.error(LOGGER, format(" Monitoring Request Signer success count has not been generated at '%s' instance! ",
                                    monitoringStatusReport.instanceIp()));
                            throw new TestFailException(format("Monitoring Request Signer success count has not been generated at '%s' instance!",
                                    monitoringStatusReport.instanceIp()));
                        }
                    });
                } else {
                    Log.error(LOGGER, format(" Cannot find Monitoring Request Signer success count at '%s' instance! ", monitoringStatusReport.instanceIp()));
                    throw new TestFailException(format("Cannot find Monitoring Request Signer success count at '%s' instance!",
                            monitoringStatusReport.instanceIp()));
                }
            } catch (IOException | IllegalStateException e) {
                Log.error(LOGGER, " Cannot parse Common Monitoring Status Report JSON! ");
                throw new TestFailException("Cannot parse Common Monitoring Status Report JSON: ", e);
            }
        }
        return requestSignerSuccessCount;
    }

    public <T extends CloudbreakTestDto> T checkFilesystemFreeBytesGeneratedMetric(T testDto, List<String> instanceIps) {
        List<String> initialSuccessCount = checkRequestSignerSuccessCount(instanceIps);
        int successCountByIp = initialSuccessCount.toArray().length;
        createHugheFilesForFilesystemFreeBytesMetric(instanceIps);
        removeHugheFilesForFilesystemFreeBytesMetric(instanceIps);
        List<String> finalSuccessCount = checkRequestSignerSuccessCount(instanceIps);

        IntStream.range(0, successCountByIp).forEach(index -> {
            if (Integer.parseInt(initialSuccessCount.get(index)) < Integer.parseInt(finalSuccessCount.get(index))) {
                Log.log(LOGGER, format(" Monitoring Request Signer success count has been updated '%s' at '%s' instance! ", finalSuccessCount.get(index),
                        instanceIps.get(index)));
            } else {
                Log.error(LOGGER, format(" Monitoring Request Signer success count has not been updated at '%s' instance! ",
                        instanceIps.get(index)));
                throw new TestFailException(format("Monitoring Request Signer success count has not been updated at '%s' instance!",
                        instanceIps.get(index)));
            }
        });

        return testDto;
    }

    public <T extends CloudbreakTestDto> T checkCipherSuiteConfiguration(T testDto, List<String> instanceIps) {

        for (Entry<String, String> service : EncryptionProfileAssertions.MONITORING_SERVICES_CONFIG_PATH.entrySet()) {
            String serviceName = service.getKey();
            String configFilePath = service.getValue();
            String readConfigCommand = "sudo cat " + configFilePath;

            SshCommandOutcomes configContents = executeOnIps(instanceIps, readConfigCommand)
                    .requireAllSucceeded(format("Reading '%s' config file", serviceName));

            for (SshCommandOutcome result : configContents) {
                String instanceIp = result.instanceIp();

                String configContent = result.commandOutput();
                encryptionProfileAssertions.validateCipherSuitesConfiguration(serviceName, configContent, instanceIp);
                Log.log(LOGGER, format("%s cipher suites configuration validated successfully at '%s' instance!", serviceName, instanceIp));
            }
        }
        return testDto;
    }

    public FreeIpaTestDto checkCipherSuiteConfiguration(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                testDto.getTestContext());
        return checkCipherSuiteConfiguration(testDto, instanceIps);
    }

    public FreeIpaTestDto checkNetworkStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                testDto.getTestContext());
        return checkCdpNetworkStatus(testDto, instanceIps);
    }

    public <T extends CloudbreakTestDto> T checkNetworkStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return checkCdpNetworkStatus(testDto, instanceIps);
    }

    private <T extends CloudbreakTestDto> T checkCdpNetworkStatus(T testDto, List<String> instanceIps) {
        String networkStatusCommand = "sudo cdp-doctor network status --format json";
        SshCommandOutcomes networkStatusReports = executeOnIps(instanceIps, networkStatusCommand).requireAllExecuted();

        eventStatusesNotOkValidation(testDto, networkStatusReports, null);
        return testDto;
    }

    public FreeIpaTestDto checkLoggingAgentStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                testDto.getTestContext());
        return checkLoggingAgentStatus(testDto, instanceIps, true);
    }

    public <T extends CloudbreakTestDto> T checkLoggingAgentStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return checkLoggingAgentStatus(testDto, instanceIps, false);
    }

    private <T extends CloudbreakTestDto> T checkLoggingAgentStatus(T testDto, List<String> instanceIps, boolean freeIpaInstances) {
        // CB-34702: cdp-doctor validates the hardcoded production DataBus endpoints instead of the ones configured on the node, so read the configured
        // endpoints from the telemetry pillar and pass them explicitly.
        String loggingAgentNokStatusCommand = """
                DBUS=$(sudo sh -c '. activate_salt_env; salt-call pillar.get "telemetry:databusEndpoint" --output json' | jq -r '.local // empty'); \
                DBUS_S3=$(sudo sh -c '. activate_salt_env; salt-call pillar.get "telemetry:databusS3Endpoint" --output json' | jq -r '.local // empty'); \
                if [ -z "$DBUS" ] || [ -z "$DBUS_S3" ]; then \
                    echo "The configured DataBus endpoints cannot be read from the telemetry pillar!"; \
                    exit 1; \
                fi; \
                if sudo cdp-doctor service status --format json | jq -e '.infraServices[] | select(.name == "minifi")' > /dev/null 2>&1; then \
                    AGENT=minifi; \
                else \
                    AGENT=fluentd; \
                fi; \
                STATUS=$(sudo cdp-doctor $AGENT status --format json --databus-url $DBUS --databus-s3-url $DBUS_S3 | tail -1); \
                if ! echo "$STATUS" | jq -e . > /dev/null 2>&1; then \
                    echo "The 'cdp-doctor $AGENT status' report is not a valid JSON: $STATUS"; \
                    exit 1; \
                fi; \
                echo "$STATUS" | jq -r '.. | objects | to_entries | map(select(.value == "NOK"))[] | .key'\
                """;
        SshCommandOutcomes loggingAgentNokStatusReports = executeOnIps(instanceIps, loggingAgentNokStatusCommand)
                .requireAllSucceeded("Logging agent status check");

        // CB-34157: the FreeIPA instances of our Azure E2E environments cannot reach public endpoints, so the S3 addresses that cdp-doctor probes for
        // these two statuses are unreachable on them. The Data Lake and the Data Hub instances of the same environments do reach those addresses, so
        // they keep asserting both statuses, and this needs to be revisited once the FreeIPA instances can reach public endpoints as well.
        Set<String> skippedStatuses = freeIpaInstances && CloudPlatform.AZURE.name().equalsIgnoreCase(commonCloudProperties.getCloudProvider())
                ? AZURE_SKIPPED_FREEIPA_LOGGING_AGENT_STATUSES
                : Set.of();
        for (SshCommandOutcome statusReport : loggingAgentNokStatusReports) {
            String loggingAgentNotOkStatuses = StringUtils.defaultString(statusReport.commandOutput()).lines()
                    .map(StringUtils::trimToEmpty)
                    .filter(StringUtils::isNotEmpty)
                    .filter(status -> !skippedStatuses.contains(status))
                    .collect(Collectors.joining(", "));
            if (StringUtils.isNotBlank(loggingAgentNotOkStatuses)) {
                Log.error(LOGGER, format(" There is 'Not OK' logging agent status %s is present on '%s' instance! ", loggingAgentNotOkStatuses,
                        statusReport.instanceIp()));
                throw new TestFailException(format("There is 'Not OK' logging agent status %s is present on '%s' instance!",
                        loggingAgentNotOkStatuses, statusReport.instanceIp()));
            }
        }
        return testDto;
    }

    public FreeIpaTestDto checkCdpServiceStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                testDto.getTestContext());
        return doCheckCdpServiceStatus(testDto, instanceIps, List.of("infraServices", "freeipaServices"));
    }

    public <T extends CloudbreakTestDto> T checkCdpServiceStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return doCheckCdpServiceStatus(testDto, instanceIps, List.of("infraServices", "cmServices"));
    }

    private <T extends CloudbreakTestDto> T doCheckCdpServiceStatus(T testDto, List<String> instanceIps, List<String> statusCategories) {
        statusCategories.forEach(statusCategory -> {
            String cdpServiceStatusCommand =
                    format("sudo cdp-doctor service status --format json | jq -r '.%s[] | \"\\(.name) \\(.status)\"'", statusCategory);
            for (SshCommandOutcome statusReport : executeOnIps(instanceIps, cdpServiceStatusCommand).requireAllExecuted()) {
                String servicesStatuses = statusReport.commandOutput();
                if (!statusReport.returnCodeSuccess() || StringUtils.isBlank(servicesStatuses)) {
                    throw new TestFailException(format("CDP Services '%s' status cannot be checked on '%s' instance: %s", statusCategory,
                            statusReport.instanceIp(), servicesStatuses));
                }
                if (StringUtils.containsIgnoreCase(servicesStatuses, " NOK")) {
                    Log.error(LOGGER, format(" There is 'Not OK' CDP Services status %s is present on '%s' instance! ", servicesStatuses,
                            statusReport.instanceIp()));
                    throw new TestFailException(format("There is 'Not OK' CDP Services status %s is present on '%s' instance!", servicesStatuses,
                            statusReport.instanceIp()));
                }
            }
        });
        return testDto;
    }

    public FreeIpaTestDto checkSystemctlServiceStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient,
            Map<String, Boolean> serviceStatusesByName) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                testDto.getTestContext());
        return checkSystemctlServiceStatus(testDto, instanceIps, serviceStatusesByName);
    }

    public <T extends CloudbreakTestDto> T checkSystemctlServiceStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            Map<String, Boolean> serviceStatusesByName) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return checkSystemctlServiceStatus(testDto, instanceIps, serviceStatusesByName);
    }

    public <T extends CloudbreakTestDto> T checkSystemctlServiceStatusOnPrimaryGateway(T testDto, List<InstanceGroupV4Response> instanceGroups,
            Map<String, Boolean> serviceStatusesByName) {
        List<String> instanceIps = getPrimaryGatewayIps(instanceGroups, false);
        return checkSystemctlServiceStatus(testDto, instanceIps, serviceStatusesByName);
    }

    private <T extends CloudbreakTestDto> T checkSystemctlServiceStatus(T testDto, List<String> instanceIps, Map<String, Boolean> serviceStatusesByName) {
        serviceStatusesByName.forEach((serviceName, serviceStatus) -> {
            String systemctlServiceStatusCommand = format("systemctl is-active %s", serviceName);
            SshCommandOutcomes systemctlServiceStatuses = executeOnIps(instanceIps, systemctlServiceStatusCommand).requireAllExecuted();
            systemctlServiceStatuses.forEach(result -> {
                String status = result.commandOutput().trim();
                if (serviceStatus != "active".equals(status)) {
                    String message = format(" systemctl service %s status on node %s is %s, but expected to be %s ",
                            serviceName, result.instanceIp(), status, serviceStatus ? "active" : "not active");
                    Log.error(LOGGER, message);
                    throw new TestFailException(message);
                }
            });
        });
        return testDto;
    }

    public void validateDnsZoneAndCnameEntryInFreeIpa(FreeIpaTestDto freeIpaTestDto, TestContext tc, FreeIpaClient freeipaClient, String dnsZone,
            String cName) {

        String kinitCmd = String.format("echo '%s' | kinit %s && klist -fea", tc.getWorkloadPassword(), tc.getWorkloadUserName());
        String findDnsRecordCmd = String.format("ipa dnsrecord-find %s %s", dnsZone, cName);

        String[] cmds = {kinitCmd, findDnsRecordCmd};
        String environmentCrn = freeIpaTestDto.getEnvironmentCrn();
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false,
                freeIpaTestDto.getTestContext());

        for (String cmd : cmds) {
            executeOnIps(instanceIps, cmd).requireAllSucceeded(format("'ipa dnsrecord-find %s %s' check", dnsZone, cName));
        }
    }
}