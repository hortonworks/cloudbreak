package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.EnvironmentAware;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.salt.SaltFunctionReport;
import com.sequenceiq.it.cloudbreak.salt.SaltHighstateReport;
import com.sequenceiq.it.cloudbreak.salt.SaltStateReport;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshJClientActions extends SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientActions.class);

    private List<String> getFreeIpaInstanceGroupIps(InstanceMetadataType istanceMetadataType, String environmentCrn, FreeIpaClient freeipaClient,
            boolean publicIp) {
        return freeipaClient.getDefaultClient().getFreeIpaV1Endpoint()
                .describe(environmentCrn).getInstanceGroups().stream()
                .filter(instanceGroup -> instanceGroup.getType().equals(InstanceGroupType.MASTER))
                .map(InstanceGroupResponse::getMetaData)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(instanceMetaData -> instanceMetaData.getInstanceType().equals(istanceMetadataType))
                .map(instanceMetaData -> {
                    LOGGER.info("The selected Instance Type [{}] and the available Private IP [{}] and Public IP [{}]. {} ip will be used!",
                            instanceMetaData.getInstanceType(), instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(),
                            publicIp ? "Public" : "Private");
                    return publicIp ? instanceMetaData.getPublicIp() : instanceMetaData.getPrivateIp();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> getInstanceGroupIps(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, boolean publicIp) {
        List<String> instanceIPs = new ArrayList<>();

        hostGroupNames.forEach(hostGroupName -> {
            List<String> instanceGroupIpList = instanceGroups.stream()
                    .filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                    .map(InstanceGroupV4Response::getMetadata)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(x -> publicIp && StringUtils.isNotEmpty(x.getPublicIp()) ? x.getPublicIp() : x.getPrivateIp())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            assert !instanceGroupIpList.isEmpty();
            LOGGER.info("The selected Instance Group [{}] and the available {} IPs [{}].",
                    hostGroupName, publicIp ? "Public" : "Private", instanceGroupIpList);
            instanceIPs.addAll(instanceGroupIpList);
        });

        return instanceIPs;
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
        Map<String, Long> filesByIp = getFreeIpaInstanceGroupIps(istanceMetadataType, environmentCrn, freeipaClient, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executefileListCommand(ip, user, password, fileListCommand)));

        validateRequiredNumberOfFiles(filesByIp, filePath, requiredNumberOfFiles);
        return testDto;
    }

    public void checkAwsEphemeralDisksMounted(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, String mountDirPrefix) {
        Map<String, Pair<Integer, String>> deviceMountPointMappingsByIp = getDeviceMountPointMappingsByIp(instanceGroups, hostGroupNames, "hadoopfs");
        Map<String, Pair<Integer, String>> deviceDiskTypeMappingsByIp = getDeviceDiskTypeMappingsByIp(instanceGroups, hostGroupNames);

        for (Entry<String, Pair<Integer, String>> node : deviceDiskTypeMappingsByIp.entrySet()) {
            Map<String, String> ephemeralDisks = new Json(node.getValue().getValue()).getMap().entrySet().stream()
                    .filter(e -> String.valueOf(e.getValue()).contains("Amazon EC2 NVMe Instance Storage"))
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            if (ephemeralDisks.isEmpty()) {
                LOGGER.error("Instance store volume missing from node with IP {}!", node.getKey());
                throw new TestFailException(format("Instance store volume missing from node with IP %s!", node.getKey()));
            }

            if (deviceMountPointMappingsByIp.get(node.getKey()) == null) {
                LOGGER.error("No device mount point mappings found for node with IP {}!", node.getKey());
                throw new TestFailException(format("No device mount point mappings found for node with IP %s!", node.getKey()));
            }
            Map<String, String> mountPoints = new Json(deviceMountPointMappingsByIp.get(node.getKey()).getValue()).getMap().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            for (String device : ephemeralDisks.keySet()) {
                String mountPoint = mountPoints.get(device);
                if (mountPoint == null) {
                    LOGGER.error("No mount point found for ephemeral device {} on node with IP {}!", device, node.getKey());
                    throw new TestFailException(format("No mount point found for device %s on node with IP %s!", device, node.getKey()));
                } else if (!mountPoint.contains(mountDirPrefix)) {
                    LOGGER.error("Ephemeral device {} incorrectly mounted to {} on node with IP {}!", device, mountPoint, node.getKey());
                    throw new TestFailException(format("Ephemeral device %s incorrectly mounted to %s on node with IP %s!",
                            device, mountPoint, node.getKey()));
                }
            }
        }
    }

    public Set<String> getAwsEphemeralVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        Map<String, Pair<Integer, String>> deviceMountPointMappingsByIp = getDeviceMountPointMappingsByIp(instanceGroups, hostGroupNames, "hadoopfs");
        Map<String, Pair<Integer, String>> deviceDiskTypeMappingsByIp = getDeviceDiskTypeMappingsByIp(instanceGroups, hostGroupNames);

        Map<String, String> mountPoints = deviceDiskTypeMappingsByIp.entrySet().stream().findFirst()
                .stream()
                .map(node -> new Json(deviceMountPointMappingsByIp.get(node.getKey()).getValue()).getMap().entrySet())
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

        return deviceDiskTypeMappingsByIp.entrySet().stream().findFirst()
                .stream()
                .map(node -> new Json(node.getValue().getValue()).getMap().entrySet().stream()
                        .filter(e -> String.valueOf(e.getValue()).contains("Amazon EC2 NVMe Instance Storage"))
                        .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue()))))
                .map(Map::keySet)
                .flatMap(Set::stream)
                .map(mountPoints::get)
                .collect(Collectors.toSet());
    }

    public void checkAzureTemporalDisksMounted(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, String mountDir) {
        Map<String, Pair<Integer, String>> deviceMountPointMappingsByIp = getDeviceMountPointMappingsByIp(instanceGroups, hostGroupNames, mountDir);
        deviceMountPointMappingsByIp.forEach((ip, outPair) -> {
            if (StringUtils.isBlank(outPair.getValue()) || outPair.getKey() != 0) {
                LOGGER.error("No mount point found '{}' on node with IP {}!", mountDir, ip);
                throw new TestFailException(format("No mount point found '%s' on node with IP %s!", mountDir, ip));
            } else if (!StringUtils.containsIgnoreCase(outPair.getValue(), mountDir)) {
                LOGGER.error("Device incorrectly mounted to '{}' on node with IP {}!", mountDir, ip);
                throw new TestFailException(format("Device incorrectly mounted to '%s' on node with IP %s!", mountDir, ip));
            }
        });
    }

    private Map<String, Pair<Integer, String>> getDeviceMountPointMappingsByIp(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            String mountDir) {
        String diskMountPointListCmd =
                "df | grep " + mountDir + " | awk '{print $1,$6}' | " +
                        "sed -e \"s/\\(.*\\) \\(.*\\)/\\\"\\1\\\":\\\"\\2\\\"/\" | paste -s -d ',' | sed 's/.*/{\\0}/'";
        return getInstanceGroupIps(instanceGroups, hostGroupNames, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, diskMountPointListCmd)));
    }

    private Map<String, Pair<Integer, String>> getDeviceDiskTypeMappingsByIp(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        String diskTypeListCmd =
                "sudo nvme list | grep dev | awk '{print $1,$3,$4,$5,$6,$7}' | " +
                        "sed -e \"s/\\([^ ]*\\) \\(.*\\)/\\\"\\1\\\":\\\"\\2\\\"/\" | paste -s -d ',' | sed 's/.*/{\\0}/'";
        return getInstanceGroupIps(instanceGroups, hostGroupNames, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, diskTypeListCmd)));
    }

    public Map<String, Pair<Integer, String>> executeSshCommandOnHost(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            String sshCommand, boolean publicIp) {
        return getInstanceGroupIps(instanceGroups, hostGroupNames, publicIp).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, sshCommand)));
    }

    public SdxTestDto checkNoOutboundInternetTraffic(SdxTestDto testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        getInstanceGroupIps(instanceGroups, hostGroupNames, true).forEach(this::checkNoOutboundInternetTraffic);
        return testDto;
    }

    public FreeIpaTestDto checkNoOutboundInternetTraffic(FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, testDto.getResponse().getEnvironmentCrn(), freeIpaClient, true)
                .forEach(this::checkNoOutboundInternetTraffic);
        return testDto;
    }

    private void checkNoOutboundInternetTraffic(String instanceIp) {
        Pair<Integer, String> cmdOut = executeSshCommand(instanceIp, "curl --max-time 30 cloudera.com");
        if (cmdOut.getKey() == 0) {
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
        Pair<Integer, String> cmdOut = executeSshCommand(instanceIp,
                "set -x kdestroy && echo Password123! | KRB5_TRACE=/dev/stdout kinit -V fakemockuser0 " +
                        "|| sleep 31 && kdestroy && echo Password123! | KRB5_TRACE=/dev/stdout kinit -V fakemockuser0 && klist | grep fakemockuser0");
        if (cmdOut.getKey() != 0) {
            String errorMsg = "Kinit wasn't successful on instance [" +
                    instanceIp + "] during freeipa upgrade! Cmd exit code: " + cmdOut.getKey() + ", Cmd output: " + cmdOut.getValue();
            LOGGER.error(errorMsg);
            throw new TestFailException(errorMsg);
        }
    }

    protected Pair<Integer, String> executeSshCommand(String instanceIp, String command) {
        return executeSshCommand(instanceIp, null, null, null, command);
    }

    private Pair<Integer, String> executeSshCommand(String instanceIp, String user, String password, String command) {
        return executeSshCommand(instanceIp, user, password, null, command);
    }

    private Pair<Integer, String> executeSshCommand(String instanceIp, String user, String password, String privateKeyFilePath, String command) {
        try (SSHClient sshClient = createSshClient(instanceIp, user, password, privateKeyFilePath)) {
            Pair<Integer, String> cmdOut = execute(sshClient, command);
            Log.log(LOGGER, " Command exit status '%s' and result '%s'. ", cmdOut.getKey(), cmdOut.getValue());
            return cmdOut;
        } catch (Exception e) {
            LOGGER.error("SSH fail on [{}] while executing command [{}]", instanceIp, command);
            throw new TestFailException(" SSH fail on [" + instanceIp + "] while executing command [" + command + "].", e);
        }
    }

    private long executefileListCommand(String instanceIP, String user, String password, String fileListCommand) {
        AtomicLong quantity = new AtomicLong(0);

        try {
            Pair<Integer, String> cmdOut = executeSshCommand(instanceIP, user, password, fileListCommand);

            List<String> cmdOutputValues = Stream.of(cmdOut.getValue().split("[\\r\\n\\t]")).filter(Objects::nonNull).collect(Collectors.toList());
            boolean fileFound = cmdOutputValues.stream()
                    .anyMatch(outputValue -> outputValue.strip().startsWith("/"));
            String foundFilePath = cmdOutputValues.stream()
                    .filter(outputValue -> outputValue.strip().startsWith("/")).findFirst().orElse(null);
            Log.log(LOGGER, format(" The file is present '%s' at '%s' path. ", fileFound, foundFilePath));

            quantity.set(cmdOutputValues.stream()
                    .filter(outputValue -> outputValue.strip().startsWith("/")).count());
        } catch (Exception e) {
            LOGGER.error("SSH fail on '{}' host while running command: [{}]", instanceIP, fileListCommand);
            throw new TestFailException(format(" SSH fail on '%s' host while running command: [%s]! ", instanceIP, fileListCommand), e);
        }
        return quantity.get();
    }

    public DistroXTestDto checkMeteringStatus(DistroXTestDto testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        String meteringStatusCommand = "sudo cdp-doctor metering status --format json";
        Map<String, Pair<Integer, String>> meteringStatusReportByIp = getInstanceGroupIps(instanceGroups, hostGroupNames, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, meteringStatusCommand)));

        for (Entry<String, Pair<Integer, String>> meteringStatusReport : meteringStatusReportByIp.entrySet()) {
            List<Integer> heartbeatEventCounts = new Json(meteringStatusReport.getValue().getValue()).getMap().entrySet().stream()
                    .filter(status -> String.valueOf(status.getKey()).contains("heartbeatEventCount"))
                    .map(Entry::getValue).collect(Collectors.toList())
                        .stream()
                        .map(countObject -> (Integer) countObject)
                        .collect(Collectors.toList());
            Log.log(LOGGER, format(" Found '%s' Metering Heartbeat Events at '%s' instance. ", heartbeatEventCounts, meteringStatusReport.getKey()));
            if (CollectionUtils.isEmpty(heartbeatEventCounts) || heartbeatEventCounts.contains(0)) {
                Log.error(LOGGER, format(" Metering Heartbeat Events does NOT generated on '%s' instance! ", meteringStatusReport.getKey()));
                throw new TestFailException(format("Metering Heartbeat Events does NOT generated on '%s' instance!", meteringStatusReport.getKey()));
            }
        }

        for (Entry<String, Pair<Integer, String>> meteringStatusReport : meteringStatusReportByIp.entrySet()) {
            List<String> heartbeatStatusesNotOk = new Json(meteringStatusReport.getValue().getValue()).getMap().entrySet().stream()
                    .filter(status -> String.valueOf(status.getValue()).contains("NOK"))
                    .map(Entry::getKey).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(heartbeatStatusesNotOk)) {
                heartbeatStatusesNotOk.forEach(event -> {
                    if (StringUtils.containsIgnoreCase(event, "databusReachable")) {
                        Log.log(LOGGER, format(" Found 'databusReachable' status is not OK at '%s' instance. However this is acceptable!",
                                meteringStatusReport.getKey()));
                    } else {
                        Log.error(LOGGER, format(" There is 'Not OK' Metering Heartbeat status %s is present on '%s' instance! ", event,
                                meteringStatusReport.getKey()));
                        throw new TestFailException(format("There is 'Not OK' Metering Heartbeat status %s is present on '%s' instance!", event,
                                meteringStatusReport.getKey()));
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
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);
        return checkMonitoringStatus(testDto, instanceIps, verifyMetricNames, acceptableNokNames);
    }

    /**
     * Creating new pre-warmed images with Common Monitoring is still in progress. So in the meantime we can install the latest version on the VMs manually.
     *
     * @param instanceGroups An instance group is a collection of virtual machine (VM) instances that you can manage as a single entity.
     * @param hostGroupNames A host group logically grouped hosts regardless of any features that they might or might not have in common.
     *                          For example: hosts do not have to have the same architecture, configuration, or storage.
     */
    private void updateMonitoring(List<String> instanceIps) {
        instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip,
                        "sudo yum update --disablerepo=* --enablerepo=cdp-infra-tools --assumeyes --skip-broken > /dev/null 2>&1")));
    }

    public <T extends CloudbreakTestDto> T checkMonitoringStatus(T testDto, List<String> instanceIps, List<String> verifyMetricNames,
            List<String> acceptableNokNames) {

        updateMonitoring(instanceIps);

        String monitoringStatusCommand = format("sudo cdp-doctor monitoring status -m %s --format json", StringUtils.join(verifyMetricNames, ","));
        Map<String, Pair<Integer, String>> monitoringStatusReportByIp = instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, monitoringStatusCommand)));
        for (Entry<String, Pair<Integer, String>> monitoringStatusReport : monitoringStatusReportByIp.entrySet()) {
            try {
                List<String> statusCategories = List.of("services", "scrapping", "metrics");
                Map<String, Map<String, String>> fetchedMonitoringStatus = JsonUtil.readValue(monitoringStatusReport.getValue().getValue(),
                        new TypeReference<Map<String, Map<String, String>>>() { });
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
                                        "However this is acceptable! ", statusCategory.toUpperCase(), service, monitoringStatusReport.getKey()));
                            } else {
                                Log.error(LOGGER, format(" Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance! ", statusCategory.toUpperCase(),
                                        service, monitoringStatusReport.getKey()));
                                throw new TestFailException(format("Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance!",
                                        statusCategory.toUpperCase(), service, monitoringStatusReport.getKey()));
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
        String allocateBigFiles = "sudo seq -f %%04.0f 4 | xargs -I '{}' fallocate -l 5G test_file'{}'.txt";

        instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip,
                        StringUtils.join(List.of(listDisk, allocateBigFiles, listDisk), " && "))));

        waitingFor(Duration.ofMinutes(1), "Waiting for Monitoring 'node_filesystem_free_bytes' metric to be generated has been interrupted");
    }

    private void removeHugheFilesForFilesystemFreeBytesMetric(List<String> instanceIps) {
        String removeAllocatedFiles = "sudo rm -rf test_file{0001..0004}.txt";
        String listDisk = "sudo df -h /";

        instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip,
                        StringUtils.join(List.of(listDisk, removeAllocatedFiles, listDisk), " && "))));

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
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);
        return checkFilesystemFreeBytesGeneratedMetric(testDto, instanceIps);
    }

    private List<String> checkRequestSignerSuccessCount(List<String> instanceIps) {
        List<String> requestSignerSuccessCount = new ArrayList<>();
        String monitoringStatusCommand = "sudo cdp-doctor monitoring status -m node_filesystem_free_bytes --format json";
        Map<String, Pair<Integer, String>> monitoringStatusReportByIp = instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, monitoringStatusCommand)));
        for (Entry<String, Pair<Integer, String>> monitoringStatusReport : monitoringStatusReportByIp.entrySet()) {
            try {
                String requestSignerCategory = "request-signer";
                Map<String, Map<String, String>> fetchedMonitoringStatus = JsonUtil.readValue(monitoringStatusReport.getValue().getValue(),
                        new TypeReference<Map<String, Map<String, String>>>() { });
                List<String> successCount = fetchedMonitoringStatus.entrySet().stream()
                        .filter(categories -> requestSignerCategory.equalsIgnoreCase(categories.getKey()))
                        .flatMap(selectedCategory -> selectedCategory.getValue().entrySet().stream()
                                .filter(servicesInCategory -> "success_count".equalsIgnoreCase(servicesInCategory.getKey())))
                        .map(Entry::getValue)
                        .collect(Collectors.toList());
                if (!successCount.isEmpty()) {
                    Log.log(LOGGER, format(" Monitoring Request Signer success count is '%s' at '%s' instance. ", successCount,
                            monitoringStatusReport.getKey()));
                    successCount.forEach(count -> {
                        if (Integer.parseInt(count) > 0) {
                            Log.log(LOGGER, format(" Found Monitoring Request Signer success count is '%s' at '%s' instance! ", count,
                                    monitoringStatusReport.getKey()));
                            requestSignerSuccessCount.add(count);
                        } else {
                            Log.error(LOGGER, format(" Monitoring Request Signer success count has not been generated at '%s' instance! ",
                                    monitoringStatusReport.getKey()));
                            throw new TestFailException(format("Monitoring Request Signer success count has not been generated at '%s' instance!",
                                    monitoringStatusReport.getKey()));
                        }
                    });
                } else {
                    Log.error(LOGGER, format(" Cannot find Monitoring Request Signer success count at '%s' instance! ", monitoringStatusReport.getKey()));
                    throw new TestFailException(format("Cannot find Monitoring Request Signer success count at '%s' instance!",
                            monitoringStatusReport.getKey()));
                }
            } catch (IOException | IllegalStateException e) {
                Log.error(LOGGER, " Cannot parse Common Monitoring Status Report JSON! ");
                throw new TestFailException("Cannot parse Common Monitoring Status Report JSON: ", e);
            }
        }
        return requestSignerSuccessCount;
    }

    public <T extends CloudbreakTestDto> T checkFilesystemFreeBytesGeneratedMetric(T testDto, List<String> instanceIps) {
        updateMonitoring(instanceIps);
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

    public CloudbreakTestDto getSaltExecutionMetrics(TestContext testContext, CloudbreakTestDto testDto, MicroserviceClient client,
            String workingDirectoryLocation, String serviceName) {

        String saltMasterIp = getSaltMasterIp(testDto, client, serviceName);
        if (!saltMasterIp.isBlank()) {
            String extractSaltMetricsCommand = "sudo -- bash -c \"source activate_salt_env; " +
                    "salt-run jobs.list_jobs search_function=state.highstate | grep -E '^[0-9]{20}:$' | sed 's/.$//' > salt_jids_" + serviceName + ".txt; " +
                    "if [[ -s salt_jids_" + serviceName + ".txt ]]; then while read jid; " +
                    "do salt-run jobs.lookup_jid \\$jid --out=json > salt_job_result_\\$jid.json; " +
                    "done < salt_jids_" + serviceName + ".txt; " +
                    "zip salt_execution_metrics_" + serviceName + ".zip salt_jids_" + serviceName + ".txt salt_job_result_*.json; " +
                    "chmod 744 salt_execution_metrics_" + serviceName + ".zip; fi;\"";
            Pair<Integer, String> cmdOut = executeSshCommand(saltMasterIp, extractSaltMetricsCommand);
            LOGGER.info("SSH test result on IP: [{}]: Return code: [{}], Result: {}", saltMasterIp, cmdOut.getLeft(), cmdOut.getRight());

            try {
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
        try {
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                File newFile = newFile(destinationDirectory, zipEntry);

                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();

                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
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
            Map<String, Map<String, SaltFunctionReport>> map = new ObjectMapper().readValue(jsonString, new TypeReference<>() { });

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
