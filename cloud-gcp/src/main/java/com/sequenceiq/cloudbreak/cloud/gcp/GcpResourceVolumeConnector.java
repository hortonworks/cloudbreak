package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.DEVICE_NAME_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Snapshot;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.DiskTypeChangeResumePoint;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpCreateDiskParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskAttachmentParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskCreationSpec;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskPlan;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateRetryService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpInstanceRetrievalService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResizeDiskParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpReusedDisk;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpSnapshotParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeUpdateResult;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.IndexingDeviceNameGenerator;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.VolumeInfo;

@Service
public class GcpResourceVolumeConnector implements ResourceVolumeConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceVolumeConnector.class);

    private static final String DISK_URL = "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s";

    private static final String SNAPSHOT_URL = "https://www.googleapis.com/compute/v1/projects/%s/global/snapshots/%s";

    private static final String READ_WRITE_MODE = "READ_WRITE";

    private static final int MAX_DISK_NAME_LENGTH = 63;

    private static final int HASH_LENGTH = 8;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Inject
    private GcpDiskUpdateService gcpDiskUpdateService;

    @Inject
    private GcpDiskUpdateRetryService gcpDiskUpdateRetryService;

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private GcpInstanceRetrievalService gcpInstanceRetrievalService;

    @Inject
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    /**
     * Updates GCP additional/data disks. GCP has no in-place disk-type change API, so the two update kinds take
     * different paths: a {@code diskType} change migrates each disk by snapshotting it, creating a new disk of the
     * target type from that snapshot and swapping it onto the instance (see
     * {@link #changeDiskVolumesType}); a resize (no {@code diskType}) grows each disk in place via
     * {@link #resizeDiskVolumes}. Both fan the per-disk work out concurrently and aggregate any failures.
     *
     * @param cloudStack     the cloud stack being modified; carries the instance templates used to resolve the disk
     *                       encryption key for the disk-type change (customer-supplied CSEK cannot be read back from
     *                       the provider)
     * @param cloudResources the disk-set resources being modified; each carries the availability zone and the
     *                       {@link VolumeSetAttributes} used to resolve the zone of every disk being updated
     */
    @Override
    public Map<String, VolumeUpdateResult> updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size,
            CloudStack cloudStack, List<CloudResource> cloudResources) throws Exception {
        if (diskType != null) {
            return changeDiskVolumesType(authenticatedContext, volumeIds, diskType, size, cloudStack, cloudResources);
        } else {
            return resizeDiskVolumes(authenticatedContext, volumeIds, size, cloudResources);
        }
    }

    /**
     * Resizes GCP additional/data disks to the requested size. GCP only supports zonal, per-disk resize
     * ({@code compute.disks().resize}), so each disk is resized via {@link GcpDiskUpdateRetryService} and the
     * per-disk requests are submitted concurrently on the {@code intermediateBuilderExecutor}. Any disks that
     * fail are aggregated into the thrown exception, so a rerun of the disk update flow re-attempts only the
     * disks that have not yet been grown.
     */
    private Map<String, VolumeUpdateResult> resizeDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, int size,
            List<CloudResource> cloudResources) throws Exception {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String projectId = gcpContext.getProjectId();
        LOGGER.info("Resizing {} GCP disk(s) to {} GB in project {}: {}", volumeIds.size(), size, projectId, volumeIds);
        Map<String, DiskResizeTarget> volumeIdToTarget = buildVolumeIdToTargetMap(cloudResources);
        Map<String, String> failedVolumes = new LinkedHashMap<>();
        List<DiskOperationFuture<List<CloudResourceStatus>>> resizeOperations = new ArrayList<>();
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
            Callable<List<CloudResourceStatus>> resizeTask = () -> gcpDiskUpdateRetryService.resizeDisk(params, gcpContext);
            resizeOperations.add(new DiskOperationFuture<>(volumeId, intermediateBuilderExecutor.submit(resizeTask)));
        }

        awaitDiskOperations("resize", resizeOperations, failedVolumes, result -> { });

        String failureMessage = buildFailureMessage("resize", failedVolumes, "The disk update can be rerun to retry the failed disks.");
        if (failureMessage != null) {
            throw new CloudbreakServiceException(failureMessage);
        }
        LOGGER.info("Successfully resized all {} GCP disk(s) to {} GB.", resizeOperations.size(), size);
        // A GCP resize grows the disk in place under the same id/device, so there are no renames to report back.
        return Map.of();
    }

    /**
     * Changes the type of GCP additional/data disks.
     * Volumes are migrated <b>in parallel</b> (one {@link CompletableFuture} per volume on the
     * {@code intermediateBuilderExecutor}), while each volume's internal 6-step sequence runs <b>synchronously</b> in
     * {@link #migrateVolumeType} so a failure can roll back exactly the steps that succeeded. Per-volume failures are
     * aggregated (first cause surfaced) and thrown as a single {@link CloudbreakServiceException}; each disk's migration
     * is atomic, so the flow can be rerun to retry only the disks that failed. Local SSD volumes are skipped.
     */
    private Map<String, VolumeUpdateResult> changeDiskVolumesType(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size,
            CloudStack cloudStack, List<CloudResource> cloudResources) {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String projectId = gcpContext.getProjectId();
        LOGGER.info("Changing the type of {} GCP disk(s) to {} in project {}: {}", volumeIds.size(), diskType, projectId, volumeIds);
        Set<String> requestedVolumeIds = new HashSet<>(volumeIds);
        Set<String> matchedVolumeIds = new HashSet<>();
        Set<String> skippedLocalSsdIds = new LinkedHashSet<>();
        Map<String, String> failedVolumes = new LinkedHashMap<>();
        DiskTypeChangeContext typeChangeContext = new DiskTypeChangeContext(compute, projectId, diskType, size, authenticatedContext, gcpContext);
        List<DiskOperationFuture<VolumeTypeChangeResult>> migrations = new ArrayList<>();
        for (CloudResource resource : cloudResources) {
            if (!ResourceType.GCP_ATTACHED_DISKSET.equals(resource.getType())) {
                continue;
            }
            VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            if (volumeSetAttributes == null || volumeSetAttributes.getVolumes() == null) {
                continue;
            }
            InstanceTemplate instanceTemplate = resolveInstanceTemplate(cloudStack, resource);
            String zone = volumeSetAttributes.getAvailabilityZone();
            boolean deleteOnTermination = Boolean.TRUE.equals(volumeSetAttributes.getDeleteOnTermination());
            for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
                if (!requestedVolumeIds.contains(volume.getId())) {
                    continue;
                }
                matchedVolumeIds.add(volume.getId());
                if (GcpDiskType.LOCAL_SSD.value().equals(volume.getType())) {
                    LOGGER.debug("Volume {} is a local SSD, skipping type change.", volume.getId());
                    skippedLocalSsdIds.add(volume.getId());
                    continue;
                }
                migrations.add(new DiskOperationFuture<>(volume.getId(),
                        CompletableFuture.supplyAsync(() -> migrateVolumeType(typeChangeContext, zone, resource, volume,
                                instanceTemplate, deleteOnTermination), intermediateBuilderExecutor)));
            }
        }

        List<VolumeTypeChangeResult> succeeded = new ArrayList<>();
        awaitDiskOperations("change the type of", migrations, failedVolumes, succeeded::add);

        String failureMessage = buildFailureMessage("change the type of", failedVolumes,
                "Each disk's type change is atomic with rollback, so the disk update can be rerun to retry the failed disks.");
        if (failureMessage != null) {
            throw new CloudbreakServiceException(failureMessage);
        }
        // Report each rename back to the caller (keyed by the volume's original id) instead of mutating the shared
        // in-memory volume attributes, so persistence is driven by an explicit result the caller owns.
        Map<String, VolumeUpdateResult> updateResults = new LinkedHashMap<>();
        for (VolumeTypeChangeResult result : succeeded) {
            String oldVolumeId = result.volume().getId();
            updateResults.put(oldVolumeId, new VolumeUpdateResult(oldVolumeId, result.newDiskName(),
                    DEVICE_NAME_PREFIX + result.newDiskName(), result.newSize()));
        }
        warnOnSkippedVolumes(requestedVolumeIds, matchedVolumeIds, skippedLocalSsdIds, migrations.size(), diskType);
        LOGGER.info("Successfully changed the type of all {} GCP disk(s) to {}.", migrations.size(), diskType);
        return updateResults;
    }

    /**
     * Warns (but does not fail) when some or all requested volume ids were not actually migrated: local SSDs cannot
     * have their type changed, and ids that match no disk on any resource are dropped. If nothing was migrated at all
     * this is likely a caller mistake, so it is surfaced loudly, but the disk update still reports success.
     */
    private void warnOnSkippedVolumes(Set<String> requestedVolumeIds, Set<String> matchedVolumeIds, Set<String> skippedLocalSsdIds,
            int migratedCount, String diskType) {
        Set<String> unmatchedIds = new LinkedHashSet<>(requestedVolumeIds);
        unmatchedIds.removeAll(matchedVolumeIds);
        if (!skippedLocalSsdIds.isEmpty()) {
            LOGGER.warn("Skipped the type change of local SSD GCP disk(s) {} to {}: local SSD types cannot be changed.", skippedLocalSsdIds, diskType);
        }
        if (!unmatchedIds.isEmpty()) {
            LOGGER.warn("Requested GCP disk type change to {} for volume id(s) {} that matched no attached disk; skipping them.", diskType, unmatchedIds);
        }
        if (migratedCount == 0 && !requestedVolumeIds.isEmpty()) {
            LOGGER.warn("No GCP disks were migrated to type {} although {} volume id(s) were requested (local SSD: {}, unmatched: {}).",
                    diskType, requestedVolumeIds.size(), skippedLocalSsdIds, unmatchedIds);
        }
    }

    /**
     * Runs the synchronous per-volume disk-type migration with strict rollback so a failure never leaves the instance
     * half-migrated. The steps and their rollback:
     * <ol>
     *   <li>Create a snapshot of the current disk. Failure &rarr; nothing to clean up, fail.</li>
     *   <li>Create the new disk (target type) from the snapshot. Failure &rarr; delete the snapshot, fail.</li>
     *   <li>Detach the old disk. Failure &rarr; delete the new disk, delete the snapshot, fail.</li>
     *   <li>Attach the new disk. Failure &rarr; re-attach the old disk, delete the new disk, delete the snapshot, fail.</li>
     *   <li>Delete the old disk. Failure &rarr; WARN and continue (not fatal).</li>
     *   <li>Delete the snapshot. Failure &rarr; WARN and continue (not fatal).</li>
     * </ol>
     * Runs on an executor thread, so all failures are surfaced as unchecked exceptions.
     *
     * <p><b>Resume-safety.</b> A JVM death mid-migration (pod eviction) can leave a disk in any intermediate state, and
     * the disk-update flow reruns this method with the <b>old</b> volume id (the DB is written only after all disks
     * succeed). Rather than assume a clean start, this method first asks
     * {@link GcpDiskUpdateRetryService#resolveDiskTypeChangeResumePoint} which of five states the disks are actually in
     * &mdash; branching on the instance's real attachment list, never on mere existence &mdash; and resumes accordingly:
     * <ul>
     *   <li>{@code FULL_MIGRATION}: no usable new disk yet &rarr; steps 1&ndash;6.</li>
     *   <li>{@code RECREATE_NEW_DISK}: a non-READY leftover new disk of ours &rarr; delete it, then steps 1&ndash;6.</li>
     *   <li>{@code RESUME_AT_DETACH}: new disk READY+unattached, old still attached &rarr; steps 3&ndash;6.</li>
     *   <li>{@code RESUME_AT_ATTACH}: new disk READY+unattached, old already detached &rarr; steps 4&ndash;6.</li>
     *   <li>{@code CLEANUP_ONLY}: new disk already attached to the target (swap completed) &rarr; delete old + snapshot.</li>
     * </ul>
     * The old disk is deleted only once the new disk is confirmed attached to the target, so a rerun never destroys the
     * instance's only copy of the data.</p>
     */
    private VolumeTypeChangeResult migrateVolumeType(DiskTypeChangeContext context, String zone, CloudResource resource,
            VolumeSetAttributes.Volume volume, InstanceTemplate instanceTemplate, boolean deleteOnTermination) {
        Compute compute = context.compute();
        String projectId = context.projectId();
        String targetType = context.targetType();
        AuthenticatedContext authenticatedContext = context.authenticatedContext();
        GcpContext gcpContext = context.gcpContext();
        String oldDiskName = volume.getId();
        String instanceId = resource.getInstanceId();
        if (StringUtils.isBlank(zone)) {
            throw new CloudbreakServiceException(
                    String.format("Could not resolve the availability zone for GCP disk %s, cannot change its type.", oldDiskName));
        }
        // GCP cannot restore a snapshot into a disk smaller than the source, so a combined type+size change floors the
        // new disk at the current size; a resize below the current size is not expressible via a type change.
        int newSize = context.size() > 0 ? Math.max(context.size(), volume.getSize()) : volume.getSize();
        String newDiskName = buildTypeChangeDiskName(oldDiskName, targetType);
        String snapshotName = buildSnapshotName(oldDiskName);
        LOGGER.info("Changing the type of GCP disk {} (instance {}, zone {}) to {} via snapshot {} and new disk {}.",
                oldDiskName, instanceId, zone, targetType, snapshotName, newDiskName);

        GcpSnapshotParameters snapshotParams =
                new GcpSnapshotParameters(compute, projectId, zone, oldDiskName, snapshotName, resource, authenticatedContext);
        GcpDiskAttachmentParameters oldDiskParams =
                new GcpDiskAttachmentParameters(compute, projectId, zone, instanceId, oldDiskName, oldDiskName, resource, authenticatedContext);
        GcpDiskAttachmentParameters newDiskParams =
                new GcpDiskAttachmentParameters(compute, projectId, zone, instanceId, newDiskName, newDiskName, resource, authenticatedContext);

        // Resume-safe dispatch: a rerun after a mid-migration crash must branch on the disks' actual attachment state,
        // never on mere existence, so the old disk is deleted only once the new disk is confirmed attached to the
        // target (see the class-level five-state table). Persist-at-end is preserved: the size we report always comes
        // from the provider-authoritative new disk when one already exists.
        DiskTypeChangeResumePoint resumePoint =
                gcpDiskUpdateRetryService.resolveDiskTypeChangeResumePoint(newDiskParams, oldDiskParams, snapshotName);
        int existingNewDiskSize = resumePoint.existingNewDisk().map(disk -> disk.getSizeGb().intValue()).orElse(newSize);
        LOGGER.info("Resume point for the type change of GCP disk {} to {} (new disk {}): {}.", oldDiskName, targetType, newDiskName, resumePoint.action());
        switch (resumePoint.action()) {
            case CLEANUP_ONLY:
                // The new disk is already attached to the target: the swap completed earlier, only cleanup remains.
                deleteDiskBestEffort(oldDiskParams, gcpContext);
                deleteSnapshotBestEffort(snapshotParams, gcpContext);
                return new VolumeTypeChangeResult(volume, newDiskName, existingNewDiskSize);
            case RESUME_AT_ATTACH:
                // The new disk is READY and the old disk already detached: attach the new disk, then delete old + snapshot.
                attachNewDiskStep(deleteOnTermination, newDiskParams, oldDiskParams, snapshotParams, gcpContext);
                deleteOldDiskStep(oldDiskParams, gcpContext, oldDiskName);
                deleteSnapshotBestEffort(snapshotParams, gcpContext);
                return new VolumeTypeChangeResult(volume, newDiskName, existingNewDiskSize);
            case RESUME_AT_DETACH:
                // The new disk is READY but the old disk is still attached: detach old, attach new, then delete old + snapshot.
                detachOldDiskStep(oldDiskParams, newDiskParams, snapshotParams, gcpContext, oldDiskName, instanceId);
                attachNewDiskStep(deleteOnTermination, newDiskParams, oldDiskParams, snapshotParams, gcpContext);
                deleteOldDiskStep(oldDiskParams, gcpContext, oldDiskName);
                deleteSnapshotBestEffort(snapshotParams, gcpContext);
                return new VolumeTypeChangeResult(volume, newDiskName, existingNewDiskSize);
            case RECREATE_NEW_DISK:
                // A leftover new disk of ours is not usable (not READY): delete it, then fall through to a full migration.
                LOGGER.info("Deleting the unusable leftover new GCP disk {} before re-running the full type change of disk {}.", newDiskName, oldDiskName);
                deleteDiskBestEffort(newDiskParams, gcpContext);
                break;
            case FULL_MIGRATION:
            default:
                break;
        }

        // Step 1: snapshot the currently attached disk.
        Snapshot snapshot = new Snapshot().setName(snapshotName);
        customGcpDiskEncryptionService.addEncryptionKeyToSnapshot(instanceTemplate, snapshot);
        createSnapshotStep(snapshotParams, snapshot, gcpContext, oldDiskName, snapshotName);

        // Step 2: create the new disk of the target type from the snapshot.
        Disk newDisk = buildDiskFromSnapshot(projectId, zone, newDiskName, targetType, newSize, snapshotName, instanceTemplate);
        GcpCreateDiskParameters createParams =
                new GcpCreateDiskParameters(compute, projectId, zone, newDiskName, newDisk, resource, authenticatedContext);
        createNewDiskStep(createParams, gcpContext, snapshotParams, targetType);

        // Step 3: detach the old disk.
        detachOldDiskStep(oldDiskParams, newDiskParams, snapshotParams, gcpContext, oldDiskName, instanceId);

        // Step 4: attach the new disk.
        attachNewDiskStep(deleteOnTermination, newDiskParams, oldDiskParams, snapshotParams, gcpContext);

        // Step 5: delete the old disk (non-fatal on failure).
        deleteOldDiskStep(oldDiskParams, gcpContext, oldDiskName);

        // Step 6: delete the snapshot (non-fatal on failure).
        deleteSnapshotBestEffort(snapshotParams, gcpContext);

        return new VolumeTypeChangeResult(volume, newDiskName, newSize);
    }

    private void createSnapshotStep(GcpSnapshotParameters snapshotParams, Snapshot snapshot, GcpContext gcpContext, String oldDiskName,
            String snapshotName) {
        try {
            gcpDiskUpdateRetryService.createSnapshot(snapshotParams, snapshot, gcpContext);
        } catch (Exception ex) {
            throw new CloudbreakServiceException(
                    String.format("Failed to create a snapshot of GCP disk %s while changing its type.", oldDiskName), ex);
        }
        LOGGER.info("Created snapshot {} of GCP disk {}.", snapshotName, oldDiskName);
    }

    private void createNewDiskStep(GcpCreateDiskParameters createParams, GcpContext gcpContext, GcpSnapshotParameters snapshotParams, String targetType) {
        try {
            Optional<CloudResource> insertOperation = gcpDiskUpdateRetryService.insertDisk(createParams);
            gcpDiskUpdateRetryService.pollDiskOperations(createParams.authenticatedContext(), gcpContext,
                    insertOperation.map(List::of).orElseGet(List::of));
        } catch (Exception ex) {
            deleteSnapshotBestEffort(snapshotParams, gcpContext);
            throw new CloudbreakServiceException(
                    String.format("Failed to create the new %s disk %s from snapshot %s while changing the type of disk %s.",
                            targetType, createParams.diskName(), snapshotParams.snapshotName(), snapshotParams.sourceDiskName()), ex);
        }
        LOGGER.info("Created new GCP disk {} of type {} from snapshot {}.", createParams.diskName(), targetType, snapshotParams.snapshotName());
    }

    private void detachOldDiskStep(GcpDiskAttachmentParameters oldDiskParams, GcpDiskAttachmentParameters newDiskParams,
            GcpSnapshotParameters snapshotParams, GcpContext gcpContext, String oldDiskName, String instanceId) {
        try {
            gcpDiskUpdateRetryService.detachDiskFromInstance(oldDiskParams, gcpContext);
        } catch (Exception ex) {
            deleteDiskBestEffort(newDiskParams, gcpContext);
            deleteSnapshotBestEffort(snapshotParams, gcpContext);
            throw new CloudbreakServiceException(
                    String.format("Failed to detach the old GCP disk %s from instance %s while changing its type.", oldDiskName, instanceId), ex);
        }
        LOGGER.info("Detached old GCP disk {} from instance {}.", oldDiskName, instanceId);
    }

    private void attachNewDiskStep(boolean deleteOnTermination, GcpDiskAttachmentParameters newDiskParams,
            GcpDiskAttachmentParameters oldDiskParams, GcpSnapshotParameters snapshotParams, GcpContext gcpContext) {
        String projectId = newDiskParams.projectId();
        String zone = newDiskParams.zone();
        String newDiskName = newDiskParams.diskName();
        String instanceId = newDiskParams.instanceId();
        String oldDiskName = oldDiskParams.diskName();
        try {
            AttachedDisk attachedDisk = buildAttachedDisk(projectId, zone, newDiskName, deleteOnTermination);
            gcpDiskUpdateRetryService.attachDiskToInstance(newDiskParams, attachedDisk, gcpContext);
        } catch (Exception ex) {
            reattachOldDiskBestEffort(projectId, zone, oldDiskName, deleteOnTermination, oldDiskParams, gcpContext);
            deleteDiskBestEffort(newDiskParams, gcpContext);
            deleteSnapshotBestEffort(snapshotParams, gcpContext);
            throw new CloudbreakServiceException(
                    String.format("Failed to attach the new GCP disk %s to instance %s while changing the type of disk %s.",
                            newDiskName, instanceId, oldDiskName), ex);
        }
        LOGGER.info("Attached new GCP disk {} to instance {}.", newDiskName, instanceId);
    }

    private void deleteOldDiskStep(GcpDiskAttachmentParameters oldDiskParams, GcpContext gcpContext, String oldDiskName) {
        try {
            gcpDiskUpdateRetryService.deleteDisk(oldDiskParams, gcpContext);
            LOGGER.info("Deleted old GCP disk {} after changing its type.", oldDiskName);
        } catch (Exception ex) {
            LOGGER.warn("Failed to delete the old GCP disk {} after changing its type; the type change succeeded, continuing.", oldDiskName, ex);
        }
    }

    /**
     * Builds the {@link Disk} for the new target-type disk restored from the snapshot. Sets the target type URL and the
     * source snapshot, plus the encryption keys: the source-snapshot key so GCP can read a customer-encrypted snapshot,
     * and the new disk's own encryption key. Both are no-ops for non-custom (or KMS/CMEK) encryption.
     */
    private Disk buildDiskFromSnapshot(String projectId, String zone, String newDiskName, String targetType, int size, String snapshotName,
            InstanceTemplate instanceTemplate) {
        Disk disk = new Disk();
        disk.setName(newDiskName);
        disk.setSizeGb((long) size);
        disk.setType(GcpDiskType.getUrl(projectId, zone, targetType));
        disk.setSourceSnapshot(String.format(SNAPSHOT_URL, projectId, snapshotName));
        customGcpDiskEncryptionService.addSourceSnapshotEncryptionKeyToDisk(instanceTemplate, disk);
        customGcpDiskEncryptionService.addEncryptionKeyToDisk(instanceTemplate, disk);
        return disk;
    }

    private void reattachOldDiskBestEffort(String projectId, String zone, String oldDiskName, boolean deleteOnTermination,
            GcpDiskAttachmentParameters oldDiskParams, GcpContext gcpContext) {
        try {
            AttachedDisk oldAttachedDisk = buildAttachedDisk(projectId, zone, oldDiskName, deleteOnTermination);
            gcpDiskUpdateRetryService.attachDiskToInstance(oldDiskParams, oldAttachedDisk, gcpContext);
            LOGGER.info("Re-attached old GCP disk {} to instance {} after a failed new-disk attach.", oldDiskName, oldDiskParams.instanceId());
        } catch (Exception ex) {
            LOGGER.warn("Failed to re-attach the old GCP disk {} to instance {} during rollback of a failed type change.",
                    oldDiskName, oldDiskParams.instanceId(), ex);
        }
    }

    private void deleteDiskBestEffort(GcpDiskAttachmentParameters diskParams, GcpContext gcpContext) {
        try {
            gcpDiskUpdateRetryService.deleteDisk(diskParams, gcpContext);
        } catch (Exception ex) {
            LOGGER.warn("Failed to delete GCP disk {} during rollback of a failed type change.", diskParams.diskName(), ex);
        }
    }

    private void deleteSnapshotBestEffort(GcpSnapshotParameters snapshotParams, GcpContext gcpContext) {
        try {
            gcpDiskUpdateRetryService.deleteSnapshot(snapshotParams, gcpContext);
        } catch (Exception ex) {
            LOGGER.warn("Failed to delete GCP snapshot {} during a disk type change; continuing.", snapshotParams.snapshotName(), ex);
        }
    }

    private InstanceTemplate resolveInstanceTemplate(CloudStack cloudStack, CloudResource resource) {
        return cloudStack.getGroups().stream()
                .filter(group -> group.getName().equals(resource.getGroup()))
                .map(Group::getReferenceInstanceTemplate)
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException(
                        String.format("Could not resolve the instance template for group %s to change the GCP disk type.", resource.getGroup())));
    }

    /**
     * Deterministic new-disk name derived from the old disk name plus a sanitized target-type suffix, so a rerun reuses
     * a half-created disk from a previous attempt (GCP disks cannot be renamed and the new disk must coexist with the old
     * during the swap). Capped at the GCP {@value #MAX_DISK_NAME_LENGTH}-character disk-name limit via
     * {@link #buildNameWithinLimit}, which keeps distinct source names distinct when truncation is needed.
     */
    private String buildTypeChangeDiskName(String oldDiskName, String targetType) {
        String suffix = "-" + targetType.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return buildNameWithinLimit(oldDiskName, suffix);
    }

    /**
     * Deterministic snapshot name derived from the old disk name plus a fixed {@code -typechange} suffix. The name is
     * intentionally stable across attempts (no timestamp) so a rerun after a partially failed attempt targets the same
     * snapshot: the existing 409-adopt / FAILED-self-heal path in {@link GcpDiskUpdateRetryService#createSnapshot} can
     * reuse or recreate it, and the matching cleanup delete removes it, so no orphaned snapshots accumulate. Capped at
     * the GCP {@value #MAX_DISK_NAME_LENGTH}-character snapshot-name limit via {@link #buildNameWithinLimit}.
     */
    private String buildSnapshotName(String oldDiskName) {
        return buildNameWithinLimit(oldDiskName, "-typechange");
    }

    /**
     * Appends {@code suffix} to {@code baseName} within the GCP {@value #MAX_DISK_NAME_LENGTH}-character limit. When the
     * combined length fits it is returned as-is; otherwise the base is truncated and a short deterministic hash of the
     * <b>full</b> original base is inserted before the suffix, so two long names that share a truncated prefix still map
     * to distinct names (avoiding a silent collision where the wrong disk/snapshot would be addressed). Deterministic:
     * the same input always yields the same name, which is what makes rerun reuse and the idempotency short-circuit work.
     */
    private String buildNameWithinLimit(String baseName, String suffix) {
        if (baseName.length() + suffix.length() <= MAX_DISK_NAME_LENGTH) {
            return baseName + suffix;
        }
        String hash = "-" + DigestUtils.sha256Hex(baseName).substring(0, HASH_LENGTH);
        String base = baseName.substring(0, MAX_DISK_NAME_LENGTH - suffix.length() - hash.length());
        // Avoid a trailing/double hyphen (GCP RFC1035 names) when the truncation lands on a hyphen boundary.
        base = StringUtils.stripEnd(base, "-");
        return base + hash + suffix;
    }

    /**
     * Creates GCP additional/data disks for an add-volumes request. GCP has no batch disk-create API, so each disk
     * insert is submitted concurrently on the {@code intermediateBuilderExecutor} via {@link GcpDiskUpdateRetryService}
     * and all the resulting operations are polled together afterwards, so no executor thread is blocked on polling. The
     * path is idempotent: {@link GcpDiskUpdateService#planDisks} reclaims still-unattached orphaned disks from a
     * previous attempt (labeled with {@link GcpConstants#CREATED_FOR_LABEL}) and only creates the remainder, and each
     * insert checks for an existing same-named disk. This is all-or-nothing: if any disk fails after retries (or the
     * poll fails), every disk submitted in this call is best-effort deleted (rolled back) and the request fails, so a
     * rerun starts from a clean state. Local SSD volumes cannot be created via add volumes on GCP.
     */
    @Override
    public List<CloudResource> createVolumes(AuthenticatedContext authenticatedContext, Group group, VolumeSetAttributes.Volume volumeRequest,
            CloudStack cloudStack, int volToAddPerInstance, List<CloudResource> cloudResources) throws CloudbreakServiceException {
        if (GcpDiskType.LOCAL_SSD.value().equals(volumeRequest.getType())) {
            throw new CloudbreakServiceException("Local SSD volumes cannot be created via add volumes on GCP.");
        }
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String projectId = gcpContext.getProjectId();
        List<CloudResource> targetResources = gcpDiskUpdateService.resolveVolumeSets(group, authenticatedContext, cloudResources);
        GcpDiskPlan plan = gcpDiskUpdateService.planDisks(authenticatedContext, group, volumeRequest, cloudStack, volToAddPerInstance,
                targetResources, compute);
        List<GcpDiskCreationSpec> specs = plan.toCreate();
        List<GcpReusedDisk> reused = plan.reused();
        LOGGER.info("Provisioning GCP disks for group {} in project {}: creating {}, reusing {} orphaned disk(s).",
                group.getName(), projectId, specs.size(), reused.size());

        List<DiskOperationFuture<Optional<CloudResource>>> insertOperations = new ArrayList<>();
        for (GcpDiskCreationSpec spec : specs) {
            GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, projectId, spec.zone(), spec.disk().getName(), spec.disk(),
                    spec.resource(), authenticatedContext);
            insertOperations.add(new DiskOperationFuture<>(spec.disk().getName(),
                    intermediateBuilderExecutor.submit(() -> gcpDiskUpdateRetryService.insertDisk(params))));
        }

        List<CloudResource> operationsToPoll = new ArrayList<>();
        Map<String, String> failedDisks = new LinkedHashMap<>();
        awaitDiskOperations("create", insertOperations, failedDisks, optional -> optional.ifPresent(operationsToPoll::add));

        String failureMessage = buildFailureMessage("create", failedDisks, null);
        if (failureMessage == null) {
            try {
                gcpDiskUpdateRetryService.pollDiskOperations(authenticatedContext, gcpContext, operationsToPoll);
            } catch (Exception ex) {
                LOGGER.warn("Polling of GCP disk create operations failed.", ex);
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                failureMessage = "Failed while waiting for GCP disk create operations to finish: " + cause.getMessage();
            }
        }

        if (failureMessage != null) {
            GcpContext deleteContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, false);
            rollbackDisks(specs, compute, projectId, authenticatedContext, deleteContext);
            throw new CloudbreakServiceException(failureMessage
                    + " All newly created disks were rolled back, so the add volumes flow can be safely rerun.");
        }

        for (GcpDiskCreationSpec spec : specs) {
            recordVolume(spec.resource(), spec.volume());
        }
        for (GcpReusedDisk reusedDisk : reused) {
            recordVolume(reusedDisk.resource(), reusedDisk.volume());
        }
        LOGGER.info("Successfully provisioned GCP disks for group {} ({} created, {} reused).", group.getName(), specs.size(), reused.size());
        return targetResources;
    }

    private void recordVolume(CloudResource resource, VolumeSetAttributes.Volume volume) {
        VolumeSetAttributes attributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        attributes.getVolumes().add(volume);
        resource.setStatus(CommonStatus.CREATED);
    }

    /**
     * Builds the aggregated failure message for a set of failed disks: the list of disk ids/names and the first
     * non-blank failure cause, with an optional trailing {@code suffix} (e.g. a rerun hint) appended. Returns
     * {@code null} when there were no failures, so a caller can use it both to decide whether to fail and to build the
     * message. The per-disk causes are already logged individually while awaiting the futures; the aggregated message
     * surfaces only the first one.
     */
    private String buildFailureMessage(String operationName, Map<String, String> failedVolumes, String suffix) {
        if (failedVolumes.isEmpty()) {
            return null;
        }
        String firstCause = failedVolumes.values().stream().filter(StringUtils::isNotBlank).findFirst().orElse(null);
        String message = String.format("Failed to %s the following GCP disks: %s.", operationName, String.join(", ", failedVolumes.keySet()));
        if (firstCause != null) {
            message += String.format(" First failure cause: %s", firstCause);
        }
        if (StringUtils.isNotBlank(suffix)) {
            message += " " + suffix;
        }
        return message;
    }

    /**
     * Waits single-threaded for every submitted per-disk future, recording each failed disk and its unwrapped cause in
     * {@code failedVolumes} (keyed by disk id/name) and passing every successful result to {@code resultConsumer}. Runs
     * on the calling thread, so {@code failedVolumes} and anything the consumer mutates are touched single-threaded; the
     * worker tasks never touch them. On interruption the interrupt flag is restored, the failure is recorded, the
     * remaining futures are cancelled, and the wait stops early instead of blocking on in-flight operations during
     * shutdown.
     *
     * @param operationName the action verb used in the log messages (e.g. {@code "resize"}, {@code "create"})
     * @param resultConsumer receives each future's successful result (e.g. to collect the operations to poll); use a
     *                       no-op when the result is not needed
     */
    private <T> void awaitDiskOperations(String operationName, List<DiskOperationFuture<T>> operations,
            Map<String, String> failedVolumes, Consumer<? super T> resultConsumer) {
        LOGGER.debug("Waiting for GCP disk {} requests ({} futures)", operationName, operations.size());
        for (DiskOperationFuture<T> operation : operations) {
            String volumeId = operation.volumeId();
            try {
                resultConsumer.accept(operation.future().get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted while waiting for GCP disk {} to {}. Cancelling remaining futures.", volumeId, operationName, ex);
                failedVolumes.put(volumeId, ex.getMessage());
                operations.forEach(op -> op.future().cancel(true));
                break;
            } catch (Exception ex) {
                LOGGER.warn("Failed to {} GCP disk {}.", operationName, volumeId, ex);
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

    /**
     * Deletes every disk submitted in this call so a failed create-volumes request leaves no orphaned disks behind.
     * All specs are attempted (not only the ones whose insert polled clean), because a disk whose insert succeeded but
     * whose poll then failed would otherwise be leaked. Deletes are submitted concurrently and their operations polled
     * together afterwards. Best-effort: {@code deleteDisk} tolerates a missing disk, so specs that were never actually
     * created are harmless; a failed deletion is logged and does not abort the remaining cleanup.
     */
    private void rollbackDisks(List<GcpDiskCreationSpec> specs, Compute compute, String projectId,
            AuthenticatedContext authenticatedContext, GcpContext deleteContext) {
        if (specs.isEmpty()) {
            return;
        }
        LOGGER.info("Rolling back {} GCP disk(s) after a failed create-volumes request.", specs.size());
        Map<Future<Optional<CloudResource>>, String> deleteFutures = new LinkedHashMap<>();
        for (GcpDiskCreationSpec spec : specs) {
            String diskName = spec.disk().getName();
            GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, projectId, spec.zone(), diskName, spec.disk(),
                    spec.resource(), authenticatedContext);
            deleteFutures.put(intermediateBuilderExecutor.submit(() -> gcpDiskUpdateRetryService.deleteDisk(params)), diskName);
        }
        List<CloudResource> deleteOperations = new ArrayList<>();
        for (Map.Entry<Future<Optional<CloudResource>>, String> entry : deleteFutures.entrySet()) {
            try {
                entry.getKey().get().ifPresent(deleteOperations::add);
            } catch (Exception ex) {
                LOGGER.warn("Failed to roll back GCP disk {} during create-volumes cleanup.", entry.getValue(), ex);
            }
        }
        try {
            gcpDiskUpdateRetryService.pollDiskOperations(authenticatedContext, deleteContext, deleteOperations);
        } catch (Exception ex) {
            LOGGER.warn("Failed while waiting for GCP disk rollback deletions to finish.", ex);
        }
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
        return fetchInstancesById(authenticatedContext, cloudStack, instanceIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getAttachedVolumeRecords(entry.getValue())));
    }

    @Override
    public Map<String, Integer> getAttachedVolumeCountPerInstance(AuthenticatedContext authenticatedContext, CloudStack cloudStack,
            Collection<String> instanceIds) {
        return fetchInstancesById(authenticatedContext, cloudStack, instanceIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> (int) entry.getValue().getDisks().stream().filter(disk -> !disk.getBoot()).count()));
    }

    private Map<String, Instance> fetchInstancesById(AuthenticatedContext authenticatedContext, CloudStack cloudStack,
            Collection<String> instanceIds) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        Compute compute = gcpComputeFactory.buildCompute(credential);
        String projectId = gcpStackUtil.getProjectId(credential);
        Map<String, String> instanceZoneMap = getInstanceZoneMap(cloudStack, instanceIds);
        String defaultZone = getDefaultZone(cloudStack);
        Map<String, Instance> instances = new HashMap<>();
        for (String instanceId : instanceIds) {
            String zone = instanceZoneMap.getOrDefault(instanceId, defaultZone);
            try {
                instances.put(instanceId, gcpInstanceRetrievalService.getInstance(compute, projectId, zone, instanceId));
            } catch (IOException e) {
                throw new CloudbreakServiceException("Failed to fetch GCP instance " + instanceId, e);
            }
        }
        return instances;
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

    private List<VolumeRecord> getAttachedVolumeRecords(Instance instance) {
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

    @Override
    public void attachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, CloudStack cloudStack)
            throws CloudbreakServiceException {
        GcpContext context = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, false);
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();
        LOGGER.debug("Attaching volumes on GCP for resources: {}", cloudResources);
        runPerVolume("attach", cloudResources, (resource, volumeSetAttributes, volume) -> {
            String zone = volumeSetAttributes.getAvailabilityZone();
            AttachedDisk attachedDisk = buildAttachedDisk(projectId, zone, volume.getId(), volumeSetAttributes.getDeleteOnTermination());
            GcpDiskAttachmentParameters parameters = new GcpDiskAttachmentParameters(compute, projectId, zone,
                    resource.getInstanceId(), volume.getId(), volume.getId(), resource, authenticatedContext);
            return gcpDiskUpdateRetryService.attachDiskToInstance(parameters, attachedDisk, context);
        });
    }

    @Override
    public void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        GcpContext context = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, false);
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();
        LOGGER.debug("Detaching volumes on GCP for resources: {}", cloudResources);
        runPerVolume("detach", cloudResources, (resource, volumeSetAttributes, volume) -> {
            GcpDiskAttachmentParameters parameters = new GcpDiskAttachmentParameters(compute, projectId,
                    volumeSetAttributes.getAvailabilityZone(), resource.getInstanceId(), volume.getId(), volume.getId(),
                    resource, authenticatedContext);
            return gcpDiskUpdateRetryService.detachDiskFromInstance(parameters, context);
        });
    }

    @Override
    public void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        GcpContext context = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, false);
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();
        LOGGER.debug("Deleting volumes on GCP for resources: {}", cloudResources);
        runPerVolume("delete", cloudResources, (resource, volumeSetAttributes, volume) -> {
            GcpDiskAttachmentParameters parameters = new GcpDiskAttachmentParameters(compute, projectId,
                    volumeSetAttributes.getAvailabilityZone(), resource.getInstanceId(), volume.getId(), volume.getId(),
                    resource, authenticatedContext);
            return gcpDiskUpdateRetryService.deleteDisk(parameters, context);
        });
    }

    /**
     * Builds the {@link AttachedDisk} for an attach request. Both the device name and the source URL use the short disk
     * name ({@code volume.getId()}), matching {@code GcpInstanceResourceBuilder#createDisk}; {@code volume.getDevice()}
     * must NOT be used as it holds the OS {@code /dev/disk/by-id/google-<id>} path, not the provider device name.
     */
    private AttachedDisk buildAttachedDisk(String projectId, String zone, String diskName, Boolean deleteOnTermination) {
        return new AttachedDisk()
                .setBoot(false)
                .setAutoDelete(Boolean.TRUE.equals(deleteOnTermination))
                .setMode(READ_WRITE_MODE)
                .setDeviceName(diskName)
                .setSource(String.format(DISK_URL, projectId, zone, diskName));
    }

    /**
     * Fans out a per-volume disk operation on the intermediate builder executor and awaits all of them single-threaded,
     * collecting failures. Local SSD volumes are skipped (they are created inline at instance build and cannot be
     * attached/detached on a running instance). If any operation failed, throws a {@link CloudbreakServiceException}
     * listing the failed disks and surfacing the first failure cause.
     */
    private void runPerVolume(String operationName, List<CloudResource> cloudResources, VolumeOperation operation) {
        List<DiskOperationFuture<List<CloudResourceStatus>>> operations = new ArrayList<>();
        for (CloudResource resource : cloudResources) {
            VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            if (volumeSetAttributes == null) {
                continue;
            }
            for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
                if (GcpDiskType.LOCAL_SSD.value().equals(volume.getType())) {
                    LOGGER.debug("Volume {} is a local SSD, skipping.", volume.getId());
                    continue;
                }
                operations.add(new DiskOperationFuture<>(volume.getId(),
                        intermediateBuilderExecutor.submit(() -> operation.apply(resource, volumeSetAttributes, volume))));
            }
        }
        Map<String, String> failedVolumes = new LinkedHashMap<>();
        awaitDiskOperations(operationName, operations, failedVolumes, result -> { });
        String failureMessage = buildFailureMessage(operationName, failedVolumes, null);
        if (failureMessage != null) {
            throw new CloudbreakServiceException(failureMessage);
        }
    }

    /**
     * Resolved resize target for a single disk: the availability zone (source of truth for the GCP resize call,
     * {@code null} when it could not be resolved) and the owning disk-set {@link CloudResource}.
     */
    private record DiskResizeTarget(String availabilityZone, CloudResource cloudResource) {
    }

    /**
     * A submitted per-disk operation future together with the disk id/name it acts on, so failures can be reported
     * against the right disk when the futures are awaited in {@link #awaitDiskOperations}.
     */
    private record DiskOperationFuture<T>(String volumeId, Future<T> future) {
    }

    /**
     * Result of a successful per-volume disk-type migration: the migrated {@link VolumeSetAttributes.Volume} (still
     * carrying its original id, so the rename can be keyed back to it), plus the new disk name and the size the new
     * disk was actually created at. {@link #changeDiskVolumesType} turns these into the {@link VolumeUpdateResult} map
     * it returns to the caller once all migrations finish; the volume attributes themselves are no longer mutated in
     * place.
     */
    private record VolumeTypeChangeResult(VolumeSetAttributes.Volume volume, String newDiskName, int newSize) {
    }

    /**
     * The invariant context shared by every per-volume {@link #migrateVolumeType} task in a single disk-type-change
     * fan-out: the GCP client, project id, requested target type, requested total size (0 when only the type changes)
     * and the authenticated/GCP contexts. Bundled into one argument so the per-volume signature stays within the
     * parameter-count limit.
     */
    private record DiskTypeChangeContext(Compute compute, String projectId, String targetType, int size,
            AuthenticatedContext authenticatedContext, GcpContext gcpContext) {
    }

    @FunctionalInterface
    private interface VolumeOperation {
        List<CloudResourceStatus> apply(CloudResource resource, VolumeSetAttributes volumeSetAttributes, VolumeSetAttributes.Volume volume) throws Exception;
    }
}
