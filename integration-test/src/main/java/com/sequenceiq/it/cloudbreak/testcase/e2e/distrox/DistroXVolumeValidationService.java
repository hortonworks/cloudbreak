package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

@Service
public class DistroXVolumeValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXVolumeValidationService.class);

    private static final Pattern FSTAB_ATTACHED_DISK_REGEX_PATTERN = Pattern.compile("^UUID=([a-z0-9-]+)\\s+(/hadoopfs/fs[0-9]+|/dbfs).*$");

    private static final Pattern UUID_RESULT_REGEX_PATTERN = Pattern.compile("(.*): UUID=\"([a-z0-9-]+)\"");

    @Inject
    private SshJClient sshJClient;

    public DistroXTestDto validateAttachedDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient cloudbreakClient) {
        Map<String, Set<ResourceV4Response>> volumeResources = getVolumeResources(tc, distroXTestDto, cloudbreakClient);
        Map<String, String> instanceIpByIdMap = InstanceUtil.getInstanceIpIdMapForAllGroup(distroXTestDto.getResponse().getInstanceGroups());
        Map<String, List<String>> validationErrorsMap = new HashMap<>();
        volumeResources.forEach((instanceId, volumeResource) -> {
            try {
                Optional<ResourceV4Response> volumeSetResource = volumeResource.stream().findFirst();
                if (volumeSetResource.isPresent()) {
                    VolumeSetAttributes volumeSetAttributes = new Json(volumeSetResource.get().getAttributes()).get(VolumeSetAttributes.class);
                    Map<String, String> mountPathByUuidMap = createMountPathByUuidMap(volumeSetAttributes.getFstab());
                    Map<String, String> uuidByDevicePathMap = getUuidByDevicePathMap(instanceIpByIdMap.get(instanceId), volumeSetAttributes.getVolumes());
                    List<String> uuids = Arrays.asList(NullUtil.getIfNotNullOtherwise(volumeSetAttributes.getUuids(), "").split(" "));
                    List<String> validationErrors = new ArrayList<>();
                    if (mountPathByUuidMap.size() != uuidByDevicePathMap.size()) {
                        validationErrors.add(String.format("The size of the mount paths do not match with the size of uuids by device paths map." +
                                " volumeset: %s, uuidByDevicePathMap: %s", volumeSetAttributes, uuidByDevicePathMap));
                    }
                    for (int i = 0; i < volumeSetAttributes.getVolumes().size(); i++) {
                        VolumeSetAttributes.Volume volume = volumeSetAttributes.getVolumes().get(i);
                        String storedUuid = uuids.get(i);
                        String deviceUuid = uuidByDevicePathMap.get(NullUtil.getIfNotNullOtherwise(volume.getDevice(), ""));
                        String mountPath = mountPathByUuidMap.getOrDefault(storedUuid, "");
                        if (!storedUuid.equals(deviceUuid)) {
                            validationErrors.add(String.format("Volume %s is not mounted properly, stored UUID: %s, device UUID: %s, mountpath: %s, volume: %s",
                                    volume, storedUuid, deviceUuid, mountPath, volume));
                        }
                        if (mountPath.contains("dbfs") && volume.getCloudVolumeUsageType() != CloudVolumeUsageType.DATABASE) {
                            validationErrors.add(String.format("Database disk's type doesn't match. stored UUID: %s, device UUID: %s, mountpath: %s, volume: %s",
                                    volume, storedUuid, deviceUuid, mountPath, volume));
                        }
                        if (mountPath.contains("hadoopfs/fs") && volume.getCloudVolumeUsageType() != CloudVolumeUsageType.GENERAL) {
                            validationErrors.add(String.format("Attached disk's type doesn't match. stored UUID: %s, device UUID: %s, mountpath: %s, volume: %s",
                                    volume, storedUuid, deviceUuid, mountPath, volume));
                        }
                    }
                    if (!validationErrors.isEmpty()) {
                        Log.error(LOGGER, "Validation errors on %s instance: %s", instanceId, validationErrors);
                        validationErrorsMap.put(instanceId, validationErrors);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Exception during validation of attached volumes", ex);
                Log.error(LOGGER, "Exception during validation of attached volumes: %s", ex);
                throw new TestFailException("Exception during validation of attached volumes", ex);
            }
        });
        if (!validationErrorsMap.isEmpty()) {
            Log.error(LOGGER, "Errors during attached volumes validation: %s", validationErrorsMap);
            throw new TestFailException("Errors during attached volumes validation: " + validationErrorsMap);
        }
        return distroXTestDto;
    }

    private Map<String, Set<ResourceV4Response>> getVolumeResources(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient client) {
        StackV4Response stackV4Response = client.getDefaultClient(testContext).stackV4Endpoint()
                .getWithResources(0L, distroXTestDto.getName(), Set.of(), testContext.getActingUserCrn().getAccountId());
        return stackV4Response.getResources().stream()
                .filter(res -> res.getResourceType() == ResourceType.GCP_ATTACHED_DISKSET)
                .collect(Collectors.groupingBy(ResourceV4Response::getInstanceId, Collectors.toSet()));
    }

    private Map<String, String> createMountPathByUuidMap(String fstab) {
        return fstab.lines()
                .map(String::trim)
                .map(FSTAB_ATTACHED_DISK_REGEX_PATTERN::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(matcher -> matcher.group(1), matcher -> matcher.group(2), (v1, v2) -> {
                    if (!Objects.equals(v1, v2)) {
                        throw new IllegalArgumentException("Duplicate fstab lines with the same uuid, but different mountpoint: " + v1 + " vs " + v2);
                    }
                    return v1;
                }));
    }

    private Map<String, String> getUuidByDevicePathMap(String ip, List<VolumeSetAttributes.Volume> volumes) {
        Pair<Integer, String> blkidResult = sshJClient.executeCommand(ip, "sudo blkid -s UUID " + volumes.stream()
                .map(VolumeSetAttributes.Volume::getDevice)
                .collect(Collectors.joining(" ")));
        return blkidResult.getRight().lines()
                .map(String::trim)
                .map(UUID_RESULT_REGEX_PATTERN::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(matcher -> matcher.group(1), matcher -> matcher.group(2)));
    }
}
