package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.DEVICE_NAME_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.IndexingDeviceNameGenerator;
import com.sequenceiq.common.model.VolumeInfo;

@Service
public class GcpResourceVolumeConnector implements ResourceVolumeConnector {

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public Map<String, Map<String, String>> getVolumeDeviceMappingByInstance(AuthenticatedContext authenticatedContext, CloudStack cloudStack,
            List<CloudResource> cloudResources) {
        return cloudResources.stream()
                .collect(Collectors.toMap(CloudResource::getInstanceId, this::getDeviceNameMap));
    }

    private Map<String, String> getDeviceNameMap(CloudResource cloudResource) {
        IndexingDeviceNameGenerator ephemeralDeviceNameGenerator = new IndexingDeviceNameGenerator(GcpConstants.NVME_DEVICE_NAME_TEMPLATE, 0);
        VolumeSetAttributes volumeSetAttributes = cloudResource.getTypedAttributes(VolumeSetAttributes.class, () -> new VolumeSetAttributes.Builder().build());
        return volumeSetAttributes.getVolumes().stream()
                .collect(Collectors.toMap(VolumeSetAttributes.Volume::getId, volume -> getDiskDeviceName(volume, ephemeralDeviceNameGenerator)));
    }

    private static String getDiskDeviceName(VolumeSetAttributes.Volume volume, IndexingDeviceNameGenerator deviceNameGenerator) {
        if (GcpDiskType.LOCAL_SSD.value().equals(volume.getType())) {
            return deviceNameGenerator.next();
        } else {
            return GcpConstants.DEVICE_NAME_PREFIX + volume.getId();
        }
    }

    @Override
    public VolumeInfo getVolumeInfoFromResourceVolume(VolumeSetAttributes.Volume volume) {
        if (volume.getType().equals(GcpDiskType.LOCAL_SSD.value())) {
            return new VolumeInfo(volume.getDevice().replace(DEVICE_NAME_PREFIX, ""), volume.getDevice(), volume.getSize(),
                volume.getCloudVolumeUsageType() == CloudVolumeUsageType.DATABASE);
        } else {
            return new VolumeInfo(volume.getId(), volume.getDevice(), volume.getSize(),
                volume.getCloudVolumeUsageType() == CloudVolumeUsageType.DATABASE);
        }
    }

    @Override
    public Map<String, List<VolumeRecord>> describeAttachedVolumes(AuthenticatedContext authenticatedContext, CloudStack cloudStack,
            Collection<String> instanceIds) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        Compute compute = gcpComputeFactory.buildCompute(credential);
        String projectId = gcpStackUtil.getProjectId(credential);
        Map<String, String> instanceZoneMap = getInstanceZoneMap(cloudStack, instanceIds);
        String defaultZone = getDefaultZone(cloudStack);
        Map<String, List<VolumeRecord>> result = new HashMap<>();
        for (String instanceId : instanceIds) {
            String zone = instanceZoneMap.getOrDefault(instanceId, defaultZone);
            try {
                Instance instance = gcpStackUtil.getComputeInstanceWithId(compute, projectId, zone, instanceId);
                result.put(instanceId, getAttachedVolumeRecords(instance));
            } catch (IOException e) {
                throw new CloudbreakServiceException("Failed to describe attached volumes for instance " + instanceId, e);
            }
        }
        return result;
    }

    private Map<String, String> getInstanceZoneMap(CloudStack cloudStack, Collection<String> instanceIds) {
        return cloudStack.getGroups().stream()
                .flatMap(group -> group.getInstances().stream())
                .filter(instance -> instanceIds.contains(instance.getInstanceId()))
                .collect(Collectors.toMap(CloudInstance::getInstanceId, CloudInstance::getAvailabilityZone, (first, second) -> first));
    }

    private String getDefaultZone(CloudStack cloudStack) {
        return cloudStack.getGroups().stream()
                .flatMap(group -> group.getInstances().stream())
                .map(CloudInstance::getAvailabilityZone)
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException("Cannot determine availability zone from cloud stack"));
    }

    private List<VolumeRecord> getAttachedVolumeRecords(Instance instance) throws IOException {
        List<VolumeRecord> attachedVolumes = new ArrayList<>();
        List<AttachedDisk> disks = instance.getDisks().stream().filter(d -> !d.getBoot()).toList();
        for (AttachedDisk attachedDisk : disks) {
            attachedVolumes.add(new VolumeRecord(
                    attachedDisk.getDeviceName(),
                    DEVICE_NAME_PREFIX + attachedDisk.getDeviceName(),
                    attachedDisk.getDiskSizeGb().intValue(),
                    attachedDisk.getType()));
        }
        return attachedVolumes;
    }
}
