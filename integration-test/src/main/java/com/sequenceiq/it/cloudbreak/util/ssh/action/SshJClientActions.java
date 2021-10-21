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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshJClientActions extends SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientActions.class);

    private List<String> getSdxInstanceGroupIps(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, boolean publicIp) {
        List<String> instanceIPs = new ArrayList<>();

        hostGroupNames.forEach(hostGroupName -> {
            InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(instanceGroups.stream()
                    .filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                    .findFirst().orElse(null)).getMetadata().stream().findFirst().orElse(null);
            assert instanceMetaDataV4Response != null;
            LOGGER.info("The selected Instance Group [{}] and the available Private IP [{}] and Public IP [{}]]. {} ip will be used.",
                    instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getPrivateIp(), instanceMetaDataV4Response.getPublicIp(),
                    publicIp ? "Public" : "Private");
            instanceIPs.add(publicIp ? instanceMetaDataV4Response.getPublicIp() : instanceMetaDataV4Response.getPrivateIp());
        });

        return instanceIPs;
    }

    private List<String> getFreeIpaInstanceGroupIps(String environmentCrn, FreeIpaClient freeipaClient, boolean publicIp) {
        List<String> instanceIPs = new ArrayList<>();

        freeipaClient.getDefaultClient().getFreeIpaV1Endpoint()
                .describe(environmentCrn).getInstanceGroups().stream()
                .forEach(ig -> {
                    InstanceMetaDataResponse instanceMetaDataResponse = ig.getMetaData().stream().findFirst().orElse(null);
                    assert instanceMetaDataResponse != null;
                    LOGGER.info("The selected Instance Group [{}] and the available Private IP [{}] and Public IP [{}]]. {} ip will be used.",
                            instanceMetaDataResponse.getInstanceGroup(), instanceMetaDataResponse.getPrivateIp(), instanceMetaDataResponse.getPublicIp(),
                            publicIp ? "Public" : "Private");
                    instanceIPs.add(publicIp ? instanceMetaDataResponse.getPublicIp() : instanceMetaDataResponse.getPrivateIp());
                });

        return instanceIPs;
    }

    private List<String> getDistroXInstanceGroupIps(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, boolean publicIp) {
        List<String> instanceIPs = new ArrayList<>();

        hostGroupNames.forEach(hostGroupName -> {
            List<String> instanceGroupIpList = instanceGroups.stream()
                    .filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                    .map(InstanceGroupV4Response::getMetadata)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(x -> publicIp ? x.getPublicIp() : x.getPrivateIp())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            assert !instanceGroupIpList.isEmpty();
            LOGGER.info("The selected Instance Group [{}] and the available {} IPs [{}].",
                    hostGroupName, publicIp ? "Public" : "Private", instanceGroupIpList);
            instanceIPs.addAll(instanceGroupIpList);
        });

        return instanceIPs;
    }

    public <T extends AbstractSdxTestDto> T checkFilesByNameAndPath(T testDto, List<InstanceGroupV4Response> instanceGroups,
            List<String> hostGroupNames, String filePath, String fileName, long requiredNumberOfFiles, String user, String password) {
        String fileListCommand = String.format("find %s -type f -name %s", filePath, fileName);
        AtomicLong quantity = new AtomicLong(0);
        String appendMessage;

        if (StringUtils.isBlank(user) && StringUtils.isBlank(password)) {
            appendMessage = String.format("with 'cloudbreak' user and defaultPrivateKeyFile from 'application.yml'");
        } else if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            appendMessage = String.format("with user: '%s' and password: '%s' and defaultPrivateKeyFile from 'application.yml'", user, password);
        } else {
            LOGGER.error("Creating SSH client is not possible, because of one of the required parameter is missing: \nuser: '{}' \npassword: '{}'",
                    user, password);
            throw new TestFailException(String.format("Creating SSH client is not possible, because of one of the required parameter is missing: " +
                            "%nuser: '%s' %npassword: '%s'",
                    user, password));
        }

        /**
         * Right now only the Private IP is available for an Instance.
         */
        getSdxInstanceGroupIps(instanceGroups, hostGroupNames, false).forEach(instanceIP -> {
            LOGGER.info("Creating SSH client on '{}' host " + appendMessage, instanceIP);
            try (SSHClient client = createSshClient(instanceIP, user, password, null)) {
                quantity.set(executefileListCommand(instanceIP, fileListCommand, client));
            } catch (Exception e) {
                LOGGER.error("Create SSH client is failing on '{}' host " + appendMessage + ", because of: {}", instanceIP, e.getMessage());
                throw new TestFailException(String.format(" Create SSH client is failing on '%s' host " + appendMessage, instanceIP), e);
            }
        });

        if (requiredNumberOfFiles == quantity.get()) {
            Log.log(LOGGER, format(" File '%s' is available at [%s] host group(s). ", filePath, hostGroupNames.toString()));
        } else {
            LOGGER.error("File '{}' is NOT available at [{}] host group(s)!", filePath, hostGroupNames.toString());
            throw new TestFailException(String.format("File '%s' is NOT available at [%s] host group(s)!", filePath, hostGroupNames.toString()));
        }
        return testDto;
    }

    public void checkEphemeralDisksMounted(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, String mountDirPrefix) {
        Map<String, Pair<Integer, String>> deviceMountPointMappingsByIp = getDeviceMountPointMappingsByIp(instanceGroups, hostGroupNames);
        Map<String, Pair<Integer, String>> deviceDiskTypeMappingsByIp = getDeviceDiskTypeMappingsByIp(instanceGroups, hostGroupNames);

        for (Entry<String, Pair<Integer, String>> node: deviceDiskTypeMappingsByIp.entrySet()) {
            Map<String, String> ephemeralDisks = new Json(node.getValue().getValue()).getMap().entrySet().stream()
                    .filter(e -> String.valueOf(e.getValue()).contains("Amazon EC2 NVMe Instance Storage"))
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            if (ephemeralDisks.isEmpty()) {
                LOGGER.error("Instance store volume missing from node with IP {}!", node.getKey());
                throw new TestFailException(String.format("Instance store volume missing from node with IP %s!", node.getKey()));
            }

            if (deviceMountPointMappingsByIp.get(node.getKey()) == null) {
                LOGGER.error("No device mount point mappings found for node with IP {}!", node.getKey());
                throw new TestFailException(String.format("No device mount point mappings found for node with IP %s!", node.getKey()));
            }
            Map<String, String> mounPoints = new Json(deviceMountPointMappingsByIp.get(node.getKey()).getValue()).getMap().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            for (String device: ephemeralDisks.keySet()) {
                String mountPoint = mounPoints.get(device);
                if (mountPoint == null) {
                    LOGGER.error("No mount point found for ephemeral device {} on node with IP {}!", device, node.getKey());
                    throw new TestFailException(String.format("No mount point found for device %s on node with IP %s!", device, node.getKey()));
                } else if (!mountPoint.contains(mountDirPrefix)) {
                    LOGGER.error("Ephemeral device {} incorrectly mounted to {} on node with IP {}!", device, mountPoint, node.getKey());
                    throw new TestFailException(String.format("Ephemeral device %s incorrectly mounted to %s on node with IP %s!",
                            device, mountPoint, node.getKey()));
                }
            }
        }
    }

    public Set<String> getEphemeralVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        Map<String, Pair<Integer, String>> deviceMountPointMappingsByIp = getDeviceMountPointMappingsByIp(instanceGroups, hostGroupNames);
        Map<String, Pair<Integer, String>> deviceDiskTypeMappingsByIp = getDeviceDiskTypeMappingsByIp(instanceGroups, hostGroupNames);

        Map<String, String> mounPoints = deviceDiskTypeMappingsByIp.entrySet().stream().findFirst()
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
                .map(mounPoints::get)
                .collect(Collectors.toSet());
    }

    public void checkNoEphemeralDisksMounted(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        Map<String, Pair<Integer, String>> deviceMountPointMappingsByIp = getDeviceMountPointMappingsByIp(instanceGroups, hostGroupNames);
        Map<String, Pair<Integer, String>> deviceDiskTypeMappingsByIp = getDeviceDiskTypeMappingsByIp(instanceGroups, hostGroupNames);

        for (Entry<String, Pair<Integer, String>> node: deviceDiskTypeMappingsByIp.entrySet()) {
            Map<String, String> ephemeralDisks = new Json(node.getValue().getValue()).getMap().entrySet().stream()
                    .filter(e -> String.valueOf(e.getValue()).contains("Amazon EC2 NVMe Instance Storage"))
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            if (!ephemeralDisks.isEmpty()) {
                LOGGER.error("Instance store volume unintentionally present on node with IP {}!", node.getKey());
                throw new TestFailException(String.format("Instance store volume unintentionally present on node with IP %s!", node.getKey()));
            }
        }

        for (Entry<String, Pair<Integer, String>> node: deviceMountPointMappingsByIp.entrySet()) {
            Map<String, String> ephemeralMounts = new Json(node.getValue().getValue()).getMap().entrySet().stream()
                    .filter(e -> String.valueOf(e.getValue()).contains("ephfs"))
                    .collect(Collectors.toMap(Entry::getKey, x -> String.valueOf(x.getValue())));

            if (!ephemeralMounts.isEmpty()) {
                LOGGER.error("Device incorrectly mounted to /hadoopfs/ephfsN on node with IP {}!", node.getKey());
                throw new TestFailException(String.format("Device incorrectly mounted to /hadoopfs/ephfsN on node with IP %s!", node.getKey()));
            }
        }
    }

    private Map<String, Pair<Integer, String>> getDeviceMountPointMappingsByIp(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        String diskMountPointListCmd =
                "df | grep hadoopfs | awk '{print $1,$6}' | " +
                        "sed -e \"s/\\(.*\\) \\(.*\\)/\\\"\\1\\\":\\\"\\2\\\"/\" | paste -s -d ',' | sed 's/.*/{\\0}/'";

        return getDistroXInstanceGroupIps(instanceGroups, hostGroupNames, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, diskMountPointListCmd)));
    }

    private Map<String, Pair<Integer, String>> getDeviceDiskTypeMappingsByIp(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        String diskTypeListCmd =
                "sudo nvme list | grep dev | awk '{print $1,$3,$4,$5,$6,$7}' | " +
                        "sed -e \"s/\\([^ ]*\\) \\(.*\\)/\\\"\\1\\\":\\\"\\2\\\"/\" | paste -s -d ',' | sed 's/.*/{\\0}/'";

        return getDistroXInstanceGroupIps(instanceGroups, hostGroupNames, false).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, diskTypeListCmd)));
    }

    public Map<String, Pair<Integer, String>> executeSshCommand(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, String sshCommand,
            boolean publicIp) {
        return getSdxInstanceGroupIps(instanceGroups, hostGroupNames, publicIp).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, sshCommand)));
    }

    public SdxTestDto checkNoOutboundInternetTraffic(SdxTestDto testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        getSdxInstanceGroupIps(instanceGroups, hostGroupNames, true).forEach(this::checkNoOutboundInternetTraffic);
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
            throw new TestFailException("Instance [" + instanceIp + "] has internet coonection but shouldn't have!");
        }
    }

    private Pair<Integer, String> executeSshCommand(String instanceIp, String command) {
        try (SSHClient sshClient = createSshClient(instanceIp, null, null, null)) {
            Pair<Integer, String> cmdOut = execute(sshClient, command);
            Log.log(LOGGER, format("Command exit status [%s] and result [%s].", cmdOut.getKey(), cmdOut.getValue()));
            return cmdOut;
        } catch (Exception e) {
            LOGGER.error("SSH fail on [{}] while executing command [{}]", instanceIp, command);
            throw new TestFailException(" SSH fail on [" + instanceIp + "] while executing command [" + command + "].", e);
        }
    }

    private long executefileListCommand(String instanceIP, String fileListCommand, SSHClient sshClient) {
        AtomicLong quantity = new AtomicLong(0);

        try {
            Pair<Integer, String> cmdOut = execute(sshClient, fileListCommand);
            Log.log(LOGGER, format(" Command exit status '%s' and result '%s'. ", cmdOut.getKey(), cmdOut.getValue()));

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
            throw new TestFailException(String.format(" SSH fail on '%s' host while running command: [%s]! ", instanceIP, fileListCommand), e);
        }
        return quantity.get();
    }
}
