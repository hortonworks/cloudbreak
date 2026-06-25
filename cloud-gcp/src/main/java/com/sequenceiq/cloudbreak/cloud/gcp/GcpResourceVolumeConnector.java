package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.DEVICE_NAME_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateRetryService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResizeDiskParameters;
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
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.VolumeInfo;

@Service
public class GcpResourceVolumeConnector implements ResourceVolumeConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceVolumeConnector.class);

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Inject
    private GcpDiskUpdateRetryService gcpDiskUpdateRetryService;

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    /**
     * Resizes GCP additional/data disks to the requested size. GCP only supports zonal, per-disk resize
     * ({@code compute.disks().resize}), so each disk is resized via {@link GcpDiskUpdateRetryService} and the
     * per-disk requests are submitted concurrently on the {@code intermediateBuilderExecutor}. Any disks that
     * fail are aggregated into the thrown exception, so a rerun of the disk update flow re-attempts only the
     * disks that have not yet been grown. Disk type changes are not supported on GCP.
     *
     * @param cloudResources the disk-set resources being modified; each carries the availability zone and the
     *                       {@link VolumeSetAttributes} used to resolve the zone of every disk being resized
     */
    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size,
            List<CloudResource> cloudResources) throws Exception {
        if (diskType != null) {
            throw new CloudbreakServiceException("Changing disk type is not supported on GCP.");
        }
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String projectId = gcpContext.getProjectId();
        LOGGER.info("Resizing {} GCP disk(s) to {} GB in project {}: {}", volumeIds.size(), size, projectId, volumeIds);
        Map<String, DiskResizeTarget> volumeIdToTarget = buildVolumeIdToTargetMap(cloudResources);
        Map<String, String> failedVolumes = new LinkedHashMap<>();
        Map<String, Future<?>> futures = new LinkedHashMap<>();
        for (String volumeId : volumeIds) {
            DiskResizeTarget target = volumeIdToTarget.get(volumeId);
            if (target == null || target.availabilityZone() == null) {
                LOGGER.warn("Could not resolve the availability zone for disk {}. Skipping resize.", volumeId);
                failedVolumes.put(volumeId, "Could not resolve the availability zone.");
                continue;
            }
            String zone = target.availabilityZone();
            LOGGER.info("Submitting resize of GCP disk {} in zone {} to {} GB.", volumeId, zone, size);
            GcpResizeDiskParameters params =
                    new GcpResizeDiskParameters(compute, projectId, zone, volumeId, size, target.cloudResource(), authenticatedContext);
            Callable<?> resizeTask = () -> gcpDiskUpdateRetryService.resizeDisk(params, gcpContext);
            futures.put(volumeId, intermediateBuilderExecutor.submit(resizeTask));
        }

        awaitVolumeResize(futures, failedVolumes, size);

        if (!failedVolumes.isEmpty()) {
            String failures = failedVolumes.entrySet().stream()
                    .map(entry -> StringUtils.isNotBlank(entry.getValue())
                            ? String.format("%s (%s)", entry.getKey(), entry.getValue())
                            : entry.getKey())
                    .collect(Collectors.joining(", "));
            throw new CloudbreakServiceException(String.format(
                    "Failed to resize the following GCP disks: %s. The disk update can be rerun to retry the failed disks.", failures));
        }
        LOGGER.info("Successfully resized all {} GCP disk(s) to {} GB.", futures.size(), size);
    }

    /**
     * Waits for every submitted resize request and records the failed disks and their cause. Runs on the calling
     * thread, so the {@code failedVolumes} map is mutated single-threaded; the worker tasks never touch it. The
     * original exception (wrapped in an {@link java.util.concurrent.ExecutionException}) is logged with its full
     * cause chain, and every cause message is captured so the aggregated failure surfaces the actual GCP error for
     * each failed disk. On interruption the remaining futures are cancelled and the wait stops early instead of
     * blocking on outstanding resizes during shutdown.
     */
    private void awaitVolumeResize(Map<String, Future<?>> futures, Map<String, String> failedVolumes, int size) {
        LOGGER.debug("Waiting for volumes resize requests ({} futures)", futures.size());
        for (Map.Entry<String, Future<?>> entry : futures.entrySet()) {
            String volumeId = entry.getKey();
            try {
                entry.getValue().get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted while waiting for GCP disk {} resize to size {} GB. Cancelling remaining futures.",
                        volumeId, size, ex);
                failedVolumes.put(volumeId, ex.getMessage());
                futures.values().forEach(future -> future.cancel(true));
                break;
            } catch (Exception ex) {
                LOGGER.warn("Failed to resize GCP disk {} to size {} GB.", volumeId, size, ex);
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                failedVolumes.put(volumeId, cause.getMessage());
            }
        }
    }

    /**
     * Maps every disk id to its {@link DiskResizeTarget}, parsing {@link VolumeSetAttributes} once per disk-set
     * resource. The zone is resolved from {@link VolumeSetAttributes#getAvailabilityZone()} (the source of truth used
     * by the GCP insert/delete calls), falling back to the resource-level column (e.g. older stacks) and left
     * {@code null} only when neither source has a zone.
     */
    private Map<String, DiskResizeTarget> buildVolumeIdToTargetMap(List<CloudResource> cloudResources) {
        Map<String, DiskResizeTarget> volumeIdToTarget = new HashMap<>();
        if (cloudResources == null) {
            return volumeIdToTarget;
        }
        List<CloudResource> diskSetResources = cloudResources.stream()
                .filter(resource -> ResourceType.GCP_ATTACHED_DISKSET.equals(resource.getType()))
                .toList();
        for (CloudResource cloudResource : diskSetResources) {
            VolumeSetAttributes volumeSetAttributes = cloudResource.getTypedAttributes(VolumeSetAttributes.class,
                    () -> new VolumeSetAttributes.Builder().build());
            if (volumeSetAttributes != null && volumeSetAttributes.getVolumes() != null) {
                String zone = StringUtils.isNotBlank(volumeSetAttributes.getAvailabilityZone())
                        ? volumeSetAttributes.getAvailabilityZone() : cloudResource.getAvailabilityZone();
                zone = StringUtils.isBlank(zone) ? null : zone;
                for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
                    volumeIdToTarget.put(volume.getId(), new DiskResizeTarget(zone, cloudResource));
                }
            }
        }
        return volumeIdToTarget;
    }

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

    /**
     * Resolved resize target for a single disk: the availability zone (source of truth for the GCP resize call,
     * {@code null} when it could not be resolved) and the owning disk-set {@link CloudResource}.
     */
    private record DiskResizeTarget(String availabilityZone, CloudResource cloudResource) {
    }
}
