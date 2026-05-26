package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.ATTRIBUTES;
import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DiskList;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskType;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Plans the GCP data disks for an add-volumes request. Its only cloud interaction is a read-only
 * {@code compute.disks().list(...)} to rediscover orphaned disks from a previous attempt (labeled with
 * {@link GcpConstants#CREATED_FOR_LABEL}); the actual, retryable disk inserts are submitted by
 * {@link GcpResourceVolumeConnector} via {@link GcpDiskUpdateRetryService}. The plan reuses reclaimable orphans and
 * only creates the remaining count, so a rerun after a partial failure does not leak disks despite the per-attempt
 * date-hashed disk names.
 */
@Service
public class GcpDiskUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDiskUpdateService.class);

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpResourceNameService gcpResourceNameService;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    /**
     * Returns the disk-set resources to add volumes to, creating fresh volume-set resources for the group when none
     * exist yet.
     */
    public List<CloudResource> resolveVolumeSets(Group group, AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) {
        if (CollectionUtils.isEmpty(cloudResources)) {
            LOGGER.info("No volume set resources in group {}, creating new volume sets", group.getName());
            return createNewVolumeSets(group, authenticatedContext);
        }
        return cloudResources;
    }

    /**
     * Plans the disks for an add-volumes request across the given resources: reclaims reusable orphaned disks
     * (labeled for the instance, still unattached on the provider, not already recorded) and builds a fresh
     * {@link GcpDiskCreationSpec} for each remaining volume. The resources' volume lists are not mutated here; the
     * connector records the created and reused volumes only after the disks are confirmed created.
     */
    public GcpDiskPlan planDisks(AuthenticatedContext authenticatedContext, Group group, VolumeSetAttributes.Volume volumeRequest,
            CloudStack cloudStack, int volToAddPerInstance, List<CloudResource> cloudResources, Compute compute) {
        String projectId = gcpStackUtil.getProjectId(authenticatedContext.getCloudCredential());
        Map<String, String> baseLabels = gcpLabelUtil.createLabelsFromTags(cloudStack);
        String stackName = authenticatedContext.getCloudContext().getName();
        List<GcpDiskCreationSpec> specs = new ArrayList<>();
        List<GcpReusedDisk> reused = new ArrayList<>();
        for (CloudResource resource : cloudResources) {
            CloudInstance cloudInstance = group.getInstances().stream()
                    .filter(instance -> instance.getInstanceId().equals(resource.getInstanceId()))
                    .findFirst()
                    .orElseThrow(() -> new CloudbreakServiceException(format("Instance %s not found in group %s",
                            resource.getInstanceId(), group.getName())));
            VolumeSetAttributes volumeSetAttributes = resource.getParameter(ATTRIBUTES, VolumeSetAttributes.class);
            String zone = resolveZone(resource, volumeSetAttributes, cloudInstance);
            int existingVolumeCount = volumeSetAttributes.getVolumes().size();
            Long privateId = cloudInstance.getTemplate().getPrivateId();
            // Label disks with the instance FQDN (globally unique, stable across reruns) so orphans can be reclaimed,
            // matching AwsAdditionalDiskCreator's "created-for=<fqdn>" tag.
            String fqdn = cloudInstance.getParameter(CloudInstance.FQDN, String.class);
            if (StringUtils.isBlank(fqdn)) {
                throw new CloudbreakServiceException(format("Instance %s in group %s has no FQDN, cannot create GCP disks for add volumes.",
                        resource.getInstanceId(), group.getName()));
            }
            String createdForLabel = gcpLabelUtil.transformLabelKeyOrValue(fqdn);

            Set<String> alreadyRecordedIds = volumeSetAttributes.getVolumes().stream()
                    .map(VolumeSetAttributes.Volume::getId).collect(Collectors.toSet());
            List<Disk> reusableDisks = discoverReusableOrphanDisks(compute, projectId, zone, createdForLabel, alreadyRecordedIds, volToAddPerInstance);
            for (Disk reusableDisk : reusableDisks) {
                VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(reusableDisk.getName(),
                        GcpConstants.DEVICE_NAME_PREFIX + reusableDisk.getName(), volumeRequest.getSize(), volumeRequest.getType(),
                        volumeRequest.getCloudVolumeUsageType());
                volume.setCloudVolumeStatus(CloudVolumeStatus.CREATED);
                reused.add(new GcpReusedDisk(resource, volume));
            }
            if (!reusableDisks.isEmpty()) {
                LOGGER.info("Reusing {} orphaned GCP disk(s) for instance {} (fqdn {}): {}", reusableDisks.size(),
                        resource.getInstanceId(), fqdn, reusableDisks.stream().map(Disk::getName).toList());
            }

            Map<String, String> labels = new HashMap<>(baseLabels);
            labels.put(GcpConstants.CREATED_FOR_LABEL, createdForLabel);
            int remaining = volToAddPerInstance - reusableDisks.size();
            for (int i = 0; i < remaining; i++) {
                int volumeIndex = existingVolumeCount + i;
                String diskName = gcpResourceNameService.attachedDisk(stackName, group.getName(), privateId, volumeIndex);
                String deviceName = GcpConstants.DEVICE_NAME_PREFIX + diskName;
                VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(diskName, deviceName,
                        volumeRequest.getSize(), volumeRequest.getType(), volumeRequest.getCloudVolumeUsageType());
                volume.setCloudVolumeStatus(CloudVolumeStatus.CREATED);
                Disk disk = buildDisk(projectId, zone, volume, labels);
                customGcpDiskEncryptionService.addEncryptionKeyToDisk(group.getReferenceInstanceTemplate(), disk);
                specs.add(new GcpDiskCreationSpec(resource, volume, disk, zone));
            }
        }
        return new GcpDiskPlan(specs, reused);
    }

    /**
     * Lists disks labeled for this instance and keeps only the genuinely unattached ones ({@code getUsers()} empty)
     * that are not already recorded on the resource, capped at {@code max}. Attachment is decided from live provider
     * state, not from the resource attributes. Best-effort: a list failure is logged and treated as "no reusable
     * disks" so the request still proceeds by creating fresh disks.
     */
    private List<Disk> discoverReusableOrphanDisks(Compute compute, String projectId, String zone, String createdForLabel,
            Set<String> alreadyRecordedIds, int max) {
        if (max <= 0) {
            return List.of();
        }
        try {
            String filter = "labels." + GcpConstants.CREATED_FOR_LABEL + " eq " + createdForLabel;
            Compute.Disks.List request = compute.disks().list(projectId, zone).setFilter(filter);
            List<Disk> reusable = new ArrayList<>();
            DiskList page;
            do {
                page = request.execute();
                List<Disk> items = page.getItems();
                if (items != null) {
                    for (Disk disk : items) {
                        boolean unattached = disk.getUsers() == null || disk.getUsers().isEmpty();
                        if (unattached && !alreadyRecordedIds.contains(disk.getName())) {
                            reusable.add(disk);
                            if (reusable.size() >= max) {
                                return reusable;
                            }
                        }
                    }
                }
                request.setPageToken(page.getNextPageToken());
            } while (page.getNextPageToken() != null);
            return reusable;
        } catch (Exception e) {
            LOGGER.warn("Could not list existing GCP disks for label {}={} in zone {} to reuse orphans; creating fresh disks instead.",
                    GcpConstants.CREATED_FOR_LABEL, createdForLabel, zone, e);
            return List.of();
        }
    }

    private List<CloudResource> createNewVolumeSets(Group group, AuthenticatedContext authenticatedContext) {
        List<CloudResource> volumeSets = new ArrayList<>();
        String stackName = authenticatedContext.getCloudContext().getName();
        for (CloudInstance instance : group.getInstances()) {
            Long privateId = instance.getTemplate().getPrivateId();
            String resourceName = gcpResourceNameService.attachedDisk(stackName, group.getName(), privateId, 0);
            VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes.Builder()
                    .withAvailabilityZone(instance.getAvailabilityZone())
                    .withDeleteOnTermination(Boolean.TRUE)
                    .withVolumes(new ArrayList<>())
                    .build();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(ATTRIBUTES, volumeSetAttributes);
            parameters.put(PRIVATE_ID, privateId);
            volumeSets.add(CloudResource.builder()
                    .withType(ResourceType.GCP_ATTACHED_DISKSET)
                    .withStatus(CommonStatus.REQUESTED)
                    .withName(resourceName)
                    .withGroup(group.getName())
                    .withInstanceId(instance.getInstanceId())
                    .withAvailabilityZone(instance.getAvailabilityZone())
                    .withParameters(parameters)
                    .build());
        }
        return volumeSets;
    }

    private static String resolveZone(CloudResource resource, VolumeSetAttributes volumeSetAttributes, CloudInstance cloudInstance) {
        if (volumeSetAttributes.getAvailabilityZone() != null) {
            return volumeSetAttributes.getAvailabilityZone();
        }
        if (resource.getAvailabilityZone() != null) {
            return resource.getAvailabilityZone();
        }
        return cloudInstance.getAvailabilityZone();
    }

    private static Disk buildDisk(String projectId, String zone, VolumeSetAttributes.Volume volume, Map<String, String> labels) {
        Disk disk = new Disk();
        disk.setName(volume.getId());
        disk.setSizeGb((long) volume.getSize());
        disk.setType(GcpDiskType.getUrl(projectId, zone, volume.getType()));
        disk.setLabels(labels);
        return disk;
    }
}
