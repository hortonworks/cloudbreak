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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

@Service
public class ClusterVolumeValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterVolumeValidationService.class);

    private static final Pattern FSTAB_ATTACHED_DISK_REGEX_PATTERN = Pattern.compile("^UUID=([a-z0-9-]+)\\s+(/hadoopfs/fs[0-9]+|/dbfs).*$");

    private static final Pattern UUID_RESULT_REGEX_PATTERN = Pattern.compile("(.*): UUID=\"([a-z0-9-]+)\"");

    private static final Pattern LSBLK_UUID_SIZE_PATTERN = Pattern.compile("^([a-z0-9-]+)\\s+(\\d+)$");

    private static final long BYTES_PER_GB = 1_073_741_824L;

    private static final Map<CloudPlatform, ResourceType> PLATFORM_DISK_RESOURCE_TYPE = Map.of(
            CloudPlatform.AWS, ResourceType.AWS_VOLUMESET,
            CloudPlatform.AZURE, ResourceType.AZURE_VOLUMESET,
            CloudPlatform.GCP, ResourceType.GCP_ATTACHED_DISKSET
    );

    @Inject
    private SshJClient sshJClient;

    public DistroXTestDto validateAttachedDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient cloudbreakClient) {
        doValidateAttachedDisks(distroXTestDto.getName(), distroXTestDto.getResponse().getInstanceGroups(), tc, cloudbreakClient);
        return distroXTestDto;
    }

    public SdxTestDto validateAttachedDisks(SdxTestDto sdxTestDto, TestContext tc, CloudbreakClient cloudbreakClient) {
        doValidateAttachedDisks(sdxTestDto.getName(), sdxTestDto.getResponse().getStackV4Response().getInstanceGroups(), tc, cloudbreakClient);
        return sdxTestDto;
    }

    public SdxInternalTestDto validateAttachedDisks(SdxInternalTestDto sdxTestDto, TestContext tc, CloudbreakClient cloudbreakClient) {
        doValidateAttachedDisks(sdxTestDto.getName(), sdxTestDto.getResponse().getStackV4Response().getInstanceGroups(), tc, cloudbreakClient);
        return sdxTestDto;
    }

    private void doValidateAttachedDisks(String stackName, List<InstanceGroupV4Response> instanceGroups, TestContext tc, CloudbreakClient cloudbreakClient) {
        Map<String, Set<ResourceV4Response>> volumeResources = getVolumeResources(tc, stackName, cloudbreakClient);
        Map<String, String> instanceIpByIdMap = InstanceUtil.getInstanceIpIdMapForAllGroup(instanceGroups);
        Map<String, List<String>> validationErrorsMap = new HashMap<>();
        CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
        volumeResources.forEach((instanceId, volumeResource) -> {
            try {
                Optional<ResourceV4Response> volumeSetResource = volumeResource.stream().findFirst();
                if (volumeSetResource.isPresent()) {
                    VolumeSetAttributes volumeSetAttributes = new Json(volumeSetResource.get().getAttributes()).get(VolumeSetAttributes.class);
                    Map<String, String> mountPathByUuidMap = createMountPathByUuidMap(volumeSetAttributes.getFstab());
                    Map<String, String> uuidByDevicePathMap = getUuidByDevicePathMap(instanceIpByIdMap.get(instanceId),
                            volumeSetAttributes.getVolumes(), cloudFunctionality);
                    List<String> uuids = Arrays.asList(NullUtil.getIfNotNullOtherwise(volumeSetAttributes.getUuids(), "").split(" "));
                    Map<String, Integer> lsblkSizes = getLsblkSizesByUuid(instanceIpByIdMap.get(instanceId));
                    Map<String, Volume> cloudVolumes = buildCloudVolumeMap(cloudFunctionality, volumeSetAttributes.getVolumes());
                    List<String> validationErrors = new ArrayList<>();
                    if (mountPathByUuidMap.size() != uuidByDevicePathMap.size()) {
                        validationErrors.add(String.format("The size of the mount paths do not match with the size of uuids by device paths map." +
                                " volumeset: %s, uuidByDevicePathMap: %s", volumeSetAttributes, uuidByDevicePathMap));
                    }
                    for (int i = 0; i < volumeSetAttributes.getVolumes().size(); i++) {
                        VolumeSetAttributes.Volume volume = volumeSetAttributes.getVolumes().get(i);
                        String storedUuid = uuids.get(i);
                        String deviceUuid = uuidByDevicePathMap.get(getDevicePath(cloudFunctionality, volume));
                        String mountPath = mountPathByUuidMap.getOrDefault(storedUuid, "");
                        if (!storedUuid.equals(deviceUuid)) {
                            validationErrors.add(String.format("Volume %s is not mounted properly, stored UUID: %s, device UUID: %s, mountpath: %s, volume: %s",
                                    volume, storedUuid, deviceUuid, mountPath, volume));
                        }
                        if (mountPath.contains("dbfs") && volume.getCloudVolumeUsageType() != CloudVolumeUsageType.DATABASE) {
                            validationErrors.add(String.format("Database disk's type doesn't match. stored UUID: %s, device UUID: %s, mountpath: %s, volume: %s",
                                    storedUuid, deviceUuid, mountPath, volume));
                        }
                        if (mountPath.contains("hadoopfs/fs") && volume.getCloudVolumeUsageType() != CloudVolumeUsageType.GENERAL) {
                            validationErrors.add(String.format("Attached disk's type doesn't match. stored UUID: %s, device UUID: %s, mountpath: %s, volume: %s",
                                    storedUuid, deviceUuid, mountPath, volume));
                        }
                        Integer expectedSize = volume.getSize();
                        Integer lsblkSize = lsblkSizes.get(storedUuid);
                        if (lsblkSize != null && !expectedSize.equals(lsblkSize)) {
                            validationErrors.add(String.format("Size mismatch on instance for volume %s: metadata=%d GB, lsblk=%d GB",
                                    volume.getId(), expectedSize, lsblkSize));
                        }
                        if (!cloudVolumes.isEmpty() && volume.getId() != null) {
                            Volume cloudVolume = cloudVolumes.get(extractVolumeId(volume.getId()));
                            if (cloudVolume != null && !expectedSize.equals(cloudVolume.getSize())) {
                                validationErrors.add(String.format("Size mismatch on provider for volume %s: metadata=%d GB, provider=%d GB",
                                        volume.getId(), expectedSize, cloudVolume.getSize()));
                            } else if (cloudVolume == null) {
                                validationErrors.add(String.format("No volume found on provider for volume %s", volume.getId()));
                            }
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
    }

    private Map<String, Set<ResourceV4Response>> getVolumeResources(TestContext testContext, String stackName, CloudbreakClient client) {
        ResourceType resourceType = PLATFORM_DISK_RESOURCE_TYPE.get(testContext.getCloudPlatform());
        StackV4Response stackV4Response = client.getDefaultClient(testContext).stackV4Endpoint()
                .getWithResources(0L, stackName, Set.of(), testContext.getActingUserCrn().getAccountId());
        return stackV4Response.getResources().stream()
                .filter(res -> res.getResourceType() == resourceType)
                .collect(Collectors.groupingBy(ResourceV4Response::getInstanceId, Collectors.toSet()));
    }

    private Map<String, String> createMountPathByUuidMap(String fstab) {
        return Objects.requireNonNullElse(fstab, "").lines()
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

    private Map<String, String> getUuidByDevicePathMap(String ip, List<VolumeSetAttributes.Volume> volumes, CloudFunctionality cloudFunctionality) {
        if (CollectionUtils.isNotEmpty(volumes)) {
            Pair<Integer, String> blkidResult = sshJClient.executeCommand(ip, "sudo blkid -s UUID " + volumes.stream()
                    .map(v -> getDevicePath(cloudFunctionality, v))
                    .collect(Collectors.joining(" ")));
            return blkidResult.getRight().lines()
                    .map(String::trim)
                    .map(UUID_RESULT_REGEX_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .collect(Collectors.toMap(matcher -> matcher.group(1).replaceFirst("scsi[1-9]", "scsi[1-9]"), matcher -> matcher.group(2)));
        } else {
            return Map.of();
        }
    }

    private String getDevicePath(CloudFunctionality cloudFunctionality, VolumeSetAttributes.Volume volume) {
        return cloudFunctionality.calculateDevicePath(volume);
    }

    private Map<String, Integer> getLsblkSizesByUuid(String ip) {
        Pair<Integer, String> result = sshJClient.executeCommand(ip, "lsblk -b -n -o UUID,SIZE");
        return result.getRight().lines()
                .map(String::trim)
                .map(LSBLK_UUID_SIZE_PATTERN::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(m -> m.group(1), m -> (int) (Long.parseLong(m.group(2)) / BYTES_PER_GB),
                        (v1, v2) -> v1));
    }

    private Map<String, Volume> buildCloudVolumeMap(CloudFunctionality cloudFunctionality, List<VolumeSetAttributes.Volume> volumes) {
        List<String> volumeIds = volumes.stream()
                .map(VolumeSetAttributes.Volume::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<String, Volume> result = Map.of();
        if (!volumeIds.isEmpty()) {
            List<Volume> cloudVolumes = cloudFunctionality.describeVolumes(volumeIds);
            if (!cloudVolumes.isEmpty() && cloudVolumes.size() == volumeIds.size()) {
                result = cloudVolumes.stream().collect(Collectors.toMap(Volume::getId, vol -> vol));
            }
        }
        return result;
    }

    private String extractVolumeId(String volumeId) {
        if (volumeId != null && volumeId.contains("/")) {
            return volumeId.substring(volumeId.lastIndexOf('/') + 1);
        }
        return volumeId;
    }
}
