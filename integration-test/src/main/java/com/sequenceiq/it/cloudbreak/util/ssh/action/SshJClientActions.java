package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static java.lang.String.format;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
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
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshJClientActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientActions.class);

    private static final String NOT_AVAILABLE = "N/A";

    @Inject
    private SshJClient sshJClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private EncryptionProfileAssertions encryptionProfileAssertions;

    private List<String> getInstanceMetadataIps(Set<InstanceMetaDataResponse> instanceMetaDatas, boolean publicIp) {
        return instanceMetaDatas.stream().map(instanceMetaDataResponse -> {
            if (publicIp) {
                return instanceMetaDataResponse.getPublicIp();
            } else {
                return instanceMetaDataResponse.getPrivateIp();
            }
        }).collect(Collectors.toList());
    }

    private List<String> getInstanceIpsFromGroups(Collection<InstanceGroupV4Response> instanceGroups, boolean publicIp) {
        List<String> instanceGroupIpList = instanceGroups.stream()
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(x -> {
                    LOGGER.info("For instance [{}] Private IP [{}] and Public IP [{}] are available. {} ip will be used!",
                            x.getInstanceId(), x.getPrivateIp(), x.getPublicIp(), publicIp && isPublicIpAvailable(x) ? "Public" : "Private");
                    return publicIp && isPublicIpAvailable(x) ? x.getPublicIp() : x.getPrivateIp();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assert !instanceGroupIpList.isEmpty();
        return instanceGroupIpList;
    }

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
                            publicIp && isPublicIpAvailable(instanceMetaData) ? "Public" : "Private");
                    return publicIp && isPublicIpAvailable(instanceMetaData) ? instanceMetaData.getPublicIp() : instanceMetaData.getPrivateIp();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> getPrimaryGatewayIps(Collection<InstanceGroupV4Response> instanceGroups, boolean publicIp) {
        return instanceGroups.stream()
                .filter(ig -> com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY == ig.getType())
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(imd -> GATEWAY_PRIMARY == imd.getInstanceType())
                .map(imd -> publicIp && isPublicIpAvailable(imd) ? imd.getPublicIp() : imd.getPrivateIp())
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
                    .map(x -> {
                        LOGGER.info("For instance [{}] in host group [{}], Private IP [{}] and Public IP [{}] are available. {} ip will be used!",
                                x.getInstanceId(), hostGroupName, x.getPublicIp(), x.getPrivateIp(), publicIp && isPublicIpAvailable(x) ? "Public" : "Private");
                        return publicIp && isPublicIpAvailable(x) ? x.getPublicIp() : x.getPrivateIp();
                    })
                    .filter(Objects::nonNull)
                    .toList();
            assert !instanceGroupIpList.isEmpty();
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

    public Map<String, Pair<Integer, String>> executeSshCommandOnAllHosts(Collection<InstanceGroupV4Response> instanceGroups,
            String sshCommand, boolean publicIp, String privateKeyFilePath) {
        return getInstanceIpsFromGroups(instanceGroups, publicIp).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, "cloudbreak", null, privateKeyFilePath, sshCommand)));
    }

    public Map<String, Pair<Integer, String>> executeSshCommandOnHost(Set<InstanceMetaDataResponse> instanceMetaDatas, String sshCommand, boolean publicIp) {
        return getInstanceMetadataIps(instanceMetaDatas, publicIp).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, sshCommand)));
    }

    public Map<String, Pair<Integer, String>> executeSshCommandOnPrimaryGateways(Collection<InstanceGroupV4Response> instanceGroups, String sshCommand,
            boolean publicIp) {
        return getPrimaryGatewayIps(instanceGroups, publicIp).stream()
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
        try (SSHClient sshClient = sshJClient.createSshClient(instanceIp, user, password, privateKeyFilePath)) {
            Pair<Integer, String> cmdOut = sshJClient.execute(sshClient, command);
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

            List<String> cmdOutputValues = Stream.of(cmdOut.getValue().split("[\\r\\n\\t]")).filter(Objects::nonNull).toList();
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

    private <T extends CloudbreakTestDto> T eventStatusesNotOkValidation(T testDto, Map<String, Pair<Integer, String>> statusReportByIp,
            String acceptableNokEventName) {
        for (Entry<String, Pair<Integer, String>> statusReport : statusReportByIp.entrySet()) {
            List<String> statusesNotOk = new Json(statusReport.getValue().getValue()).getMap().entrySet().stream()
                    .filter(status -> String.valueOf(status.getValue()).contains("NOK"))
                    .map(Entry::getKey).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(statusesNotOk)) {
                statusesNotOk.forEach(event -> {
                    if (StringUtils.isNotBlank(acceptableNokEventName) && StringUtils.containsIgnoreCase(event, acceptableNokEventName)) {
                        Log.log(LOGGER, format(" Found '%s' status is not OK at '%s' instance. However this is acceptable!", acceptableNokEventName,
                                statusReport.getKey()));
                    } else {
                        Log.error(LOGGER, format(" There is 'Not OK' status %s is present on '%s' instance! ", event,
                                statusReport.getKey()));
                        throw new TestFailException(format("There is 'Not OK' status %s is present on '%s' instance!", event,
                                statusReport.getKey()));
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

    public <T extends CloudbreakTestDto> T checkMonitoringStatus(T testDto, List<String> instanceIps, List<String> verifyMetricNames,
            List<String> acceptableNokNames) {

        String monitoringStatusCommand = format("sudo cdp-doctor monitoring status -m %s --format json", StringUtils.join(verifyMetricNames, ","));
        Map<String, Pair<Integer, String>> monitoringStatusReportByIp = instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, monitoringStatusCommand)));
        for (Entry<String, Pair<Integer, String>> monitoringStatusReport : monitoringStatusReportByIp.entrySet()) {
            try {
                List<String> statusCategories = List.of("services", "scrapping", "metrics");
                Map<String, Map<String, String>> fetchedMonitoringStatus = JsonUtil.readValue(monitoringStatusReport.getValue().getValue(),
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
                                        "However this is acceptable! ", statusCategory.toUpperCase(Locale.ROOT), service, monitoringStatusReport.getKey()));
                            } else {
                                Log.error(LOGGER, format(" Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance! ",
                                        statusCategory.toUpperCase(Locale.ROOT), service, monitoringStatusReport.getKey()));
                                throw new TestFailException(format("Found Monitoring '%s' where '%s' is 'Not OK' at '%s' instance!",
                                        statusCategory.toUpperCase(Locale.ROOT), service, monitoringStatusReport.getKey()));
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

            Map<String, Pair<Integer, String>> configContentByIp = instanceIps.stream()
                    .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, readConfigCommand)));

            for (Entry<String, Pair<Integer, String>> configEntry : configContentByIp.entrySet()) {
                String instanceIp = configEntry.getKey();
                Pair<Integer, String> result = configEntry.getValue();

                if (result.getKey() != 0) {
                    Log.error(LOGGER, format("Failed to read '%s' config file at '%s' instance! Exit code: %d, Error: %s",
                            serviceName, instanceIp, result.getKey(), result.getValue()));
                    throw new TestFailException(format("Failed to read '%s' config file at '%s' instance!",
                            serviceName, instanceIp));
                }

                String configContent = result.getValue();
                encryptionProfileAssertions.validateCipherSuitesConfiguration(serviceName, configContent, instanceIp);
                Log.log(LOGGER, format("%s cipher suites configuration validated successfully at '%s' instance!", serviceName, instanceIp));
            }
        }
        return testDto;
    }

    public FreeIpaTestDto checkCipherSuiteConfiguration(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);
        return checkCipherSuiteConfiguration(testDto, instanceIps);
    }

    public FreeIpaTestDto checkNetworkStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);
        return checkCdpNetworkStatus(testDto, instanceIps);
    }

    public <T extends CloudbreakTestDto> T checkNetworkStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return checkCdpNetworkStatus(testDto, instanceIps);
    }

    private <T extends CloudbreakTestDto> T checkCdpNetworkStatus(T testDto, List<String> instanceIps) {
        String networkStatusCommand = "sudo cdp-doctor network status --format json";
        Map<String, Pair<Integer, String>> networkStatusReportByIp = instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, networkStatusCommand)));

        eventStatusesNotOkValidation(testDto, networkStatusReportByIp, null);
        return testDto;
    }

    public FreeIpaTestDto checkFluentdStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);
        return checkFluentdStatus(testDto, instanceIps);
    }

    public <T extends CloudbreakTestDto> T checkFluentdStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        List<String> instanceIps = getInstanceGroupIps(instanceGroups, hostGroupNames, false);
        return checkFluentdStatus(testDto, instanceIps);
    }

    private <T extends CloudbreakTestDto> T checkFluentdStatus(T testDto, List<String> instanceIps) {
        String fluentdNokStatusCommand = "sudo cdp-doctor fluentd status --format json | tail -1 | " +
                "jq -r '.. | objects | to_entries | map(select(.value == \"NOK\"))[] | \"\\(.key) \\(.value)\"'";
        Map<String, Pair<Integer, String>> fluentdNokStatusReportByIp = instanceIps.stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, fluentdNokStatusCommand)));

        for (Entry<String, Pair<Integer, String>> statusReport : fluentdNokStatusReportByIp.entrySet()) {
            String fluentdNotOkStatuses = StringUtils.trimToNull(statusReport.getValue().getValue());
            if (StringUtils.isNotBlank(fluentdNotOkStatuses)) {
                Log.error(LOGGER, format(" There is 'Not OK' CDP Fluentd status %s is present on '%s' instance! ", fluentdNotOkStatuses,
                        statusReport.getKey()));
                throw new TestFailException(format("There is 'Not OK' CDP Fluentd status %s is present on '%s' instance!", fluentdNotOkStatuses,
                        statusReport.getKey()));
            }
        }
        return testDto;
    }

    public FreeIpaTestDto checkCdpServiceStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);
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
            Map<String, Pair<Integer, String>> cdpServiceStatusReportByIp = instanceIps.stream()
                    .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, cdpServiceStatusCommand)));

            for (Entry<String, Pair<Integer, String>> statusReport : cdpServiceStatusReportByIp.entrySet()) {
                String servicesStatuses = statusReport.getValue().getValue();
                if (StringUtils.containsIgnoreCase(" NOK", servicesStatuses)) {
                    Log.error(LOGGER, format(" There is 'Not OK' CDP Services status %s is present on '%s' instance! ", servicesStatuses,
                            statusReport.getKey()));
                    throw new TestFailException(format("There is 'Not OK' CDP Services status %s is present on '%s' instance!", servicesStatuses,
                            statusReport.getKey()));
                }
            }
        });
        return testDto;
    }

    public FreeIpaTestDto checkSystemctlServiceStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient,
            Map<String, Boolean> serviceStatusesByName) {
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);
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
            Map<String, Pair<Integer, String>> systemctlServiceStatusByIp = instanceIps.stream()
                    .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, systemctlServiceStatusCommand)));
            systemctlServiceStatusByIp.forEach((ip, result) -> {
                String status = result.getValue().trim();
                if (serviceStatus != "active".equals(status)) {
                    String message = format(" systemctl service %s status on node %s is %s, but expected to be %s ",
                            serviceName, ip, status, serviceStatus ? "active" : "not active");
                    Log.error(LOGGER, message);
                    throw new TestFailException(message);
                }
            });
        });
        return testDto;
    }

    public Map<String, Pair<Integer, String>> getSSLModeForExternalDBByIp(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            String privateKeyFilePath) {
        String dbSslConnectionUrlCmd = "sudo cat /srv/pillar/cloudera-manager/database.sls | grep -E 'connectionURL\":'";
        return getInstanceGroupIps(instanceGroups, hostGroupNames, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, "cloudbreak", null, privateKeyFilePath, dbSslConnectionUrlCmd)));
    }

    public List<String> executeSshCommandsOnInstances(List<?> instanceGroups, List<String> hostGroupNames, String privateKeyFilePath,
            String command) {
        List<String> instanceIps = new ArrayList<>();
        if (!instanceGroups.isEmpty() && instanceGroups.getFirst() instanceof InstanceGroupResponse) {
            instanceIps = getInstanceGroupIps((List<InstanceGroupResponse>) instanceGroups, hostGroupNames);
        } else if (!instanceGroups.isEmpty() && instanceGroups.getFirst() instanceof InstanceGroupV4Response) {
            instanceIps = getInstanceGroupIps((List<InstanceGroupV4Response>) instanceGroups, hostGroupNames, false);
        }
        return instanceIps.stream()
                .map(ip -> executeSshCommand(ip, "cloudbreak", null, privateKeyFilePath, command).getValue().toLowerCase(Locale.ROOT))
                .toList();
    }

    private List<String> getInstanceGroupIps(List<InstanceGroupResponse> instanceGroups, List<String> hostGroupNames) {
        List<String> instanceIPs = new ArrayList<>();

        hostGroupNames.forEach(hostGroupName -> {
            List<String> instanceGroupIpList = instanceGroups.stream()
                    .filter(instanceGroup -> instanceGroup.getName().contains(hostGroupName.toLowerCase(Locale.ROOT)))
                    .map(InstanceGroupResponse::getMetaData)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(x -> isPublicIpAvailable(x) ? x.getPublicIp() : x.getPrivateIp())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            assert !instanceGroupIpList.isEmpty();
            LOGGER.info("The selected Instance Group [{}] and the available IPs [{}].",
                    hostGroupName, instanceGroupIpList);
            instanceIPs.addAll(instanceGroupIpList);
        });

        return instanceIPs;
    }

    private boolean isPublicIpAvailable(InstanceMetaDataResponse instanceMetaData) {
        return StringUtils.isNotEmpty(instanceMetaData.getPublicIp()) && !NOT_AVAILABLE.equals(instanceMetaData.getPublicIp());
    }

    private boolean isPublicIpAvailable(InstanceMetaDataV4Response instanceMetaData) {
        return StringUtils.isNotEmpty(instanceMetaData.getPublicIp()) && !NOT_AVAILABLE.equals(instanceMetaData.getPublicIp());
    }

    public void validateDnsZoneAndCnameEntryInFreeIpa(FreeIpaTestDto freeIpaTestDto, TestContext tc, FreeIpaClient freeipaClient, String dnsZone,
            String cName) {

        String kinitCmd = String.format("echo '%s' | kinit %s && klist -fea", tc.getWorkloadPassword(), tc.getWorkloadUserName());
        String findDnsRecordCmd = String.format("ipa dnsrecord-find %s %s", dnsZone, cName);

        String[] cmds = {kinitCmd, findDnsRecordCmd};
        String environmentCrn = freeIpaTestDto.getEnvironmentCrn();
        List<String> instanceIps = getFreeIpaInstanceGroupIps(InstanceMetadataType.GATEWAY_PRIMARY, environmentCrn, freeipaClient, false);

        for (String ip: instanceIps) {
            for (String cmd: cmds) {
                Pair<Integer, String> results = executeSshCommand(ip, "cloudbreak", null, commonCloudProperties.getDefaultPrivateKeyFile(), cmd);
                LOGGER.info("Result of ssh: {}", results);
                if (results.getKey() != 0) {
                    throw new TestFailException(String.format("[%s] Failure after 'ipa dnsrecord-find %s %s' command:\n%s", ip, dnsZone, cName,
                            results.getValue()));
                }
            }
        }
    }
}
