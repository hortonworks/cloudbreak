package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshJClientActions extends SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientActions.class);

    private List<String> getFreeIpaInstanceGroupIps(String environmentCrn, FreeIpaClient freeipaClient, boolean publicIp) {
        List<String> instanceIPs = new ArrayList<>();

        freeipaClient.getDefaultClient().getFreeIpaV1Endpoint()
                .describe(environmentCrn).getInstanceGroups().forEach(ig -> {
                    InstanceMetaDataResponse instanceMetaDataResponse = ig.getMetaData().stream().findFirst().orElse(null);
                    assert instanceMetaDataResponse != null;
                    LOGGER.info("The selected Instance Group [{}] and the available Private IP [{}] and Public IP [{}]]. {} ip will be used.",
                            instanceMetaDataResponse.getInstanceGroup(), instanceMetaDataResponse.getPrivateIp(), instanceMetaDataResponse.getPublicIp(),
                            publicIp ? "Public" : "Private");
                    instanceIPs.add(publicIp ? instanceMetaDataResponse.getPublicIp() : instanceMetaDataResponse.getPrivateIp());
                });

        return instanceIPs;
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
                Log.log(LOGGER, " Required number (%d) of files are available at '%s' on %s instance. ", fileCount, filePath, ip);
            } else {
                LOGGER.error("Required number ({}) of files are NOT available at '{}' on {} instance!", requiredNumberOfFiles, filePath, ip);
                throw new TestFailException(format("Required number (%d) of files are NOT available at '%s' on %s instance!", requiredNumberOfFiles,
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
            String filePath, String fileName, long requiredNumberOfFiles, String user, String password) {
        String fileListCommand = format("find %s -type f -name %s", filePath, fileName);
        Map<String, Long> filesByIp = getFreeIpaInstanceGroupIps(environmentCrn, freeipaClient, false).stream()
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
        getFreeIpaInstanceGroupIps(testDto.getResponse().getEnvironmentCrn(), freeIpaClient, true)
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

    private Pair<Integer, String> executeSshCommand(String instanceIp, String command) {
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
            Log.log(LOGGER, " Command exit status '%s' and result '%s'. ", cmdOut.getKey(), cmdOut.getValue());

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
            LOGGER.info(format("heartbeatEventCounts: %s", heartbeatEventCounts));
            Log.log(LOGGER, format(" Found '%s' Metering Heartbeat Events at '%s' instance. ", heartbeatEventCounts, meteringStatusReport.getKey()));
            if (CollectionUtils.isEmpty(heartbeatEventCounts) || heartbeatEventCounts.contains(0)) {
                LOGGER.error("Metering Heartbeat Events does NOT generated on '{}' instance!", meteringStatusReport.getKey());
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
                        LOGGER.warn("Found 'databusReachable' status is not OK at '{}' instance. However this is acceptable!", meteringStatusReport.getKey());
                    } else {
                        Log.log(LOGGER, format(" Found '%s' not OK at '%s' instance. ", event, meteringStatusReport.getKey()));
                        LOGGER.error("There is 'Not OK' Metering Heartbeat status {} is present on '{}' instance!", event, meteringStatusReport.getKey());
                        throw new TestFailException(format("There is 'Not OK' Metering Heartbeat status %s is present on '%s' instance!", event,
                                meteringStatusReport.getKey()));
                    }
                });
            }
        }

        return testDto;
    }
}
