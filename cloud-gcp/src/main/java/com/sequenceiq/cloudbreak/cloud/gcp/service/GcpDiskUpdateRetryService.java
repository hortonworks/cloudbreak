package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DisksResizeRequest;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Snapshot;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskInsertOperationService.GcpDiskInsertOutcome;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpComputeBaseResourceChecker;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpExceptionUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Self-contained retry + poll service backing the add-volumes attach/detach/delete operations on GCP. Each disk
 * operation is submitted to the Compute API and its zonal {@link Operation} is polled to completion via
 * {@link SyncPollingScheduler}. Transient provider errors (429/5xx) are retried; other client errors fail fast.
 */
@Service
public class GcpDiskUpdateRetryService extends AbstractGcpComputeBaseResourceChecker implements ResourceChecker<GcpContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDiskUpdateRetryService.class);

    private static final String DISK_STATUS_READY = "READY";

    @Inject
    private ResourcePollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private GcpDiskInsertOperationService gcpDiskInsertOperationService;

    @Retryable(retryFor = GoogleJsonResponseException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public List<CloudResourceStatus> resizeDisk(GcpResizeDiskParameters params, GcpContext gcpContext) throws Exception {
        String diskDeviceName = params.diskName();
        LOGGER.info("Resizing GCP disk {} in zone {} to {} GB.", diskDeviceName, params.preferredZone(), params.size());
        DisksResizeRequest resizeRequest = new DisksResizeRequest().setSizeGb((long) params.size());
        Operation resizeOperation = params.compute().disks().resize(params.projectId(), params.preferredZone(), diskDeviceName, resizeRequest).execute();
        if (resizeOperation.getHttpErrorStatusCode() != null) {
            throw new CloudbreakServiceException(String.format("Resize request for GCP disk %s failed: %s",
                    diskDeviceName, resizeOperation.getHttpErrorMessage()));
        }
        CloudResource operationAwareCloudResource = createOperationAwareCloudResource(params.cloudResource(), resizeOperation);
        LOGGER.debug("Waiting for resize operation {} of GCP disk {} to finish.", resizeOperation.getName(), diskDeviceName);
        List<CloudResourceStatus> result = waitForOperation(params.authenticatedContext(), gcpContext, List.of(operationAwareCloudResource));
        LOGGER.info("Successfully resized GCP disk {} in zone {} to {} GB.", diskDeviceName, params.preferredZone(), params.size());
        return result;
    }

    /**
     * Creates a snapshot of a single source disk and polls the resulting <b>zonal</b> operation to completion. The
     * {@link Snapshot} body is prebuilt by the caller (mirroring how {@link #attachDiskToInstance} takes a prebuilt
     * {@link AttachedDisk}) so the caller can attach the encryption key. Retries on transient GCP errors.
     */
    @Retryable(value = CloudConnectorException.class, maxAttempts = 8,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000))
    public List<CloudResourceStatus> createSnapshot(GcpSnapshotParameters parameters, Snapshot snapshot, GcpContext context) {
        String snapshotName = parameters.snapshotName();
        String sourceDiskName = parameters.sourceDiskName();
        try {
            LOGGER.info("Creating snapshot {} of GCP disk {} in zone {}.", snapshotName, sourceDiskName, parameters.zone());
            Operation operation = parameters.compute().disks()
                    .createSnapshot(parameters.projectId(), parameters.zone(), sourceDiskName, snapshot)
                    .execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new CloudbreakServiceException(String.format("Create snapshot request for GCP disk %s failed: %s",
                        sourceDiskName, operation.getHttpErrorMessage()));
            }
            CloudResource operationAwareResource = createOperationAwareCloudResource(parameters.cloudResource(), operation);
            return waitForOperation(parameters.authenticatedContext(), context, List.of(operationAwareResource));
        } catch (GoogleJsonResponseException e) {
            if (GcpExceptionUtil.conflictException(e)) {
                // Snapshot names are unique per migration, so a 409 here means this call's own @Retryable resubmitted
                // after the first submit already created the snapshot. Adopt that in-progress snapshot instead of failing.
                return handleAlreadySubmittedSnapshot(parameters, e);
            }
            throw classifyGoogleException(e, "snapshot", sourceDiskName);
        } catch (CloudConnectorException | CloudbreakServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudbreakServiceException(String.format("Failed to create snapshot '%s' of disk '%s'.", snapshotName, sourceDiskName), e);
        }
    }

    /**
     * Handles a 409 from {@code createSnapshot} caused by this method's own retry after a successful first submit: the
     * snapshot our previous attempt started already exists. Returns an empty status list once it is {@code READY};
     * otherwise throws a {@link CloudConnectorException} so {@code @Retryable} waits (with backoff) and re-checks. A
     * snapshot that vanished (404) is rethrown so the create is retried from scratch, and a {@code FAILED} leftover is
     * self-healed: it is deleted (best-effort) and a {@link CloudConnectorException} is thrown so the deterministically
     * named snapshot is recreated on the next retry, instead of failing the whole type change on a transient failure.
     */
    private List<CloudResourceStatus> handleAlreadySubmittedSnapshot(GcpSnapshotParameters parameters, GoogleJsonResponseException conflict) {
        String snapshotName = parameters.snapshotName();
        try {
            Snapshot existing = parameters.compute().snapshots().get(parameters.projectId(), snapshotName).execute();
            String status = existing.getStatus();
            if ("READY".equals(status)) {
                LOGGER.info("Snapshot {} already exists and is READY, reusing it.", snapshotName);
                return List.of();
            }
            if ("FAILED".equals(status)) {
                LOGGER.warn("Snapshot {} already exists but is in FAILED state; deleting it so it can be recreated on retry.", snapshotName);
                deleteFailedSnapshotBestEffort(parameters);
                throw new CloudConnectorException(String.format("Snapshot '%s' existed in FAILED state and was deleted, retrying create.", snapshotName),
                        conflict);
            }
            throw new CloudConnectorException(String.format("Snapshot '%s' already exists and is not READY yet (status: %s), waiting.",
                    snapshotName, status));
        } catch (GoogleJsonResponseException getException) {
            if (getException.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("Snapshot {} conflicted on create but is now gone, retrying create.", snapshotName);
                throw new CloudConnectorException(String.format("Snapshot '%s' create conflicted but the snapshot is gone, retrying.", snapshotName),
                        getException);
            }
            throw classifyGoogleException(getException, "get snapshot", snapshotName);
        } catch (IOException getException) {
            throw new CloudConnectorException(String.format("Failed to read the status of already existing snapshot '%s', retrying.", snapshotName),
                    getException);
        }
    }

    /**
     * Best-effort delete of a {@code FAILED} snapshot leftover so the deterministically named snapshot can be recreated
     * on the next {@code createSnapshot} retry. The delete is only submitted (not polled): the retry's create will 409
     * again while the delete is still in flight and simply re-enter {@link #handleAlreadySubmittedSnapshot} with backoff,
     * so a failure to delete here is logged and swallowed rather than aborting the type change.
     */
    private void deleteFailedSnapshotBestEffort(GcpSnapshotParameters parameters) {
        String snapshotName = parameters.snapshotName();
        try {
            parameters.compute().snapshots().delete(parameters.projectId(), snapshotName).execute();
            LOGGER.info("Submitted delete of FAILED snapshot {}.", snapshotName);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("FAILED snapshot {} is already gone.", snapshotName);
            } else {
                LOGGER.warn("Could not delete FAILED snapshot {}; the next create attempt will retry.", snapshotName, e);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not delete FAILED snapshot {}; the next create attempt will retry.", snapshotName, e);
        }
    }

    /**
     * Deletes a single snapshot and polls the resulting <b>global</b> operation to completion. Tolerates a missing
     * snapshot (404) so the best-effort cleanup / rollback path is safely re-runnable. Retries on transient GCP errors.
     *
     * @return the polled resource statuses, or an empty list when the snapshot was already gone.
     */
    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public List<CloudResourceStatus> deleteSnapshot(GcpSnapshotParameters parameters, GcpContext context) {
        String snapshotName = parameters.snapshotName();
        try {
            LOGGER.info("Deleting GCP snapshot {}.", snapshotName);
            Operation operation = parameters.compute().snapshots().delete(parameters.projectId(), snapshotName).execute();
            CloudResource operationAwareResource = createOperationAwareCloudResource(parameters.cloudResource(), operation);
            return waitForOperation(parameters.authenticatedContext(), context, List.of(operationAwareResource));
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("GCP snapshot {} not found on provider, treating as already deleted.", snapshotName);
                return List.of();
            }
            throw classifyGoogleException(e, "delete snapshot", snapshotName);
        } catch (CloudConnectorException | CloudbreakServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudbreakServiceException(String.format("Failed to delete snapshot '%s'.", snapshotName), e);
        }
    }

    /**
     * Submits the insert of a single GCP disk and returns the operation-aware {@link CloudResource} to be polled later,
     * or {@link Optional#empty()} when a same-named disk already exists and is safely reusable (no insert, nothing to
     * poll). This method does not poll: the caller collects the returned resources and polls them together via
     * {@link #pollDiskOperations}, so an executor thread is never blocked on the scheduler while holding a slot.
     * Idempotent and retryable: an existing disk attached only to the target instance is reused, and an insert 409 is
     * resolved to reuse inside {@link GcpDiskInsertOperationService}.
     */
    @Retryable(retryFor = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Optional<CloudResource> insertDisk(GcpCreateDiskParameters params) throws Exception {
        String diskName = params.diskName();
        LOGGER.info("Creating GCP disk {} in zone {}.", diskName, params.zone());
        try {
            GcpDiskInsertOutcome outcome = gcpDiskInsertOperationService.insertDiskIfAbsent(params.compute(), params.projectId(), params.zone(),
                    params.disk(), diskName);
            if (outcome.existingDisk().isPresent()) {
                verifyReusable(outcome.existingDisk().get(), params);
                return Optional.empty();
            }
            Operation insertOperation = outcome.operation().orElseThrow();
            LOGGER.debug("Submitted create operation {} for GCP disk {}.", insertOperation.getName(), diskName);
            return Optional.of(createOperationAwareCloudResource(params.cloudResource(), insertOperation));
        } catch (GoogleJsonResponseException e) {
            throw classifyGoogleException(e, "create", diskName);
        }
    }

    /**
     * Attaches a single disk to its target instance. Idempotent so {@code @Retryable} re-attempts and reruns of the
     * add-volumes flow are safe: if the disk is already attached to the target instance the attach is skipped, and if
     * it is attached to a different instance the attach fails fast. Retries on transient GCP errors.
     *
     * @return the polled resource statuses, or an empty list when the disk was already attached to the target.
     */
    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public List<CloudResourceStatus> attachDiskToInstance(GcpDiskAttachmentParameters parameters, AttachedDisk attachedDisk, GcpContext context) {
        String diskName = parameters.diskName();
        try {
            if (alreadyAttachedToTargetInstance(parameters)) {
                LOGGER.info("Disk '{}' is already attached to instance '{}', skipping attach.", diskName, parameters.instanceId());
                return List.of();
            }
            Operation operation = parameters.compute().instances()
                    .attachDisk(parameters.projectId(), parameters.zone(), parameters.instanceId(), attachedDisk)
                    .execute();
            CloudResource operationAwareResource = createOperationAwareCloudResource(parameters.cloudResource(), operation);
            return waitForOperation(parameters.authenticatedContext(), context, List.of(operationAwareResource));
        } catch (GoogleJsonResponseException e) {
            throw classifyGoogleException(e, "attach", diskName);
        } catch (CloudConnectorException | CloudbreakServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudbreakServiceException(String.format("Failed to attach disk '%s' to instance '%s'.", diskName,
                    parameters.instanceId()), e);
        }
    }

    /**
     * Submits the delete of a single GCP disk and returns the operation-aware {@link CloudResource} to be polled later,
     * or {@link Optional#empty()} when the disk is already gone (404) so the rollback path is safely re-runnable. Does
     * not poll; the caller batches the poll via {@link #pollDiskOperations}.
     */
    @Retryable(retryFor = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Optional<CloudResource> deleteDisk(GcpCreateDiskParameters params) throws Exception {
        String diskName = params.diskName();
        LOGGER.info("Deleting GCP disk {} in zone {}.", diskName, params.zone());
        try {
            Operation deleteOperation = params.compute().disks().delete(params.projectId(), params.zone(), diskName).execute();
            if (deleteOperation.getHttpErrorStatusCode() != null) {
                throw new CloudbreakServiceException(String.format("Delete request for GCP disk %s failed: %s",
                        diskName, deleteOperation.getHttpErrorMessage()));
            }
            LOGGER.debug("Submitted delete operation {} for GCP disk {}.", deleteOperation.getName(), diskName);
            return Optional.of(createOperationAwareCloudResource(params.cloudResource(), deleteOperation));
        } catch (GoogleJsonResponseException e) {
            if (GcpExceptionUtil.resourceNotFoundException(e)) {
                LOGGER.info("GCP disk {} not found on provider, treating as already deleted.", diskName);
                return Optional.empty();
            }
            throw classifyGoogleException(e, "delete", diskName);
        }
    }

    /**
     * Detaches a single disk from its instance. Tolerates an already-detached or missing disk so the rollback path is
     * safely re-runnable. Retries on transient GCP errors.
     *
     * @return the polled resource statuses, or an empty list when there was nothing to detach.
     */
    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public List<CloudResourceStatus> detachDiskFromInstance(GcpDiskAttachmentParameters parameters, GcpContext context) {
        String diskName = parameters.diskName();
        try {
            if (!attachedToTargetInstance(parameters)) {
                LOGGER.info("Disk '{}' is not attached to instance '{}', skipping detach.", diskName, parameters.instanceId());
                return List.of();
            }
            Operation operation = parameters.compute().instances()
                    .detachDisk(parameters.projectId(), parameters.zone(), parameters.instanceId(), parameters.deviceName())
                    .execute();
            CloudResource operationAwareResource = createOperationAwareCloudResource(parameters.cloudResource(), operation);
            return waitForOperation(parameters.authenticatedContext(), context, List.of(operationAwareResource));
        } catch (GoogleJsonResponseException e) {
            throw classifyGoogleException(e, "detach", diskName);
        } catch (CloudConnectorException | CloudbreakServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudbreakServiceException(String.format("Failed to detach disk '%s' from instance '%s'.", diskName,
                    parameters.instanceId()), e);
        }
    }

    /**
     * Deletes a single disk. Tolerates a missing disk (404) so the rollback path is safely re-runnable. Retries on
     * transient GCP errors.
     *
     * @return the polled resource statuses, or an empty list when the disk was already gone.
     */
    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public List<CloudResourceStatus> deleteDisk(GcpDiskAttachmentParameters parameters, GcpContext context) {
        String diskName = parameters.diskName();
        try {
            Operation operation = parameters.compute().disks()
                    .delete(parameters.projectId(), parameters.zone(), diskName)
                    .execute();
            CloudResource operationAwareResource = createOperationAwareCloudResource(parameters.cloudResource(), operation);
            return waitForOperation(parameters.authenticatedContext(), context, List.of(operationAwareResource));
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("Disk '{}' not found on provider, treating as already deleted.", diskName);
                return List.of();
            }
            throw classifyGoogleException(e, "delete", diskName);
        } catch (CloudConnectorException | CloudbreakServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudbreakServiceException(String.format("Failed to delete disk '%s'.", diskName), e);
        }
    }

    private boolean alreadyAttachedToTargetInstance(GcpDiskAttachmentParameters parameters) throws Exception {
        try {
            Disk disk = parameters.compute().disks().get(parameters.projectId(), parameters.zone(), parameters.diskName()).execute();
            List<String> users = disk.getUsers();
            if (users == null || users.isEmpty()) {
                return false;
            }
            if (users.stream().anyMatch(user -> parameters.instanceId().equals(lastPathSegment(user)))) {
                return true;
            }
            throw new CloudbreakServiceException(String.format("Disk '%s' is already attached to a different instance %s, cannot attach it to '%s'.",
                    parameters.diskName(), users, parameters.instanceId()));
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("Disk '{}' not found on provider before attach, proceeding with attach.", parameters.diskName());
                return false;
            }
            throw e;
        }
    }

    /**
     * Resolves where a disk-type-change migration should resume after a possible mid-flight JVM eviction.
     * It branches on the <b>actual attachment state</b> of the new and old disks, not on existence:
     * a new disk that exists is not necessarily attached, and deleting the old disk while the new one is not yet
     * attached would destroy the instance's only copy of the data. The five outcomes are described on
     * {@link ResumeAction}.
     *
     * <p>Attachment is classified first, because attachment (not the snapshot lineage) is the authoritative signal of
     * how far a prior attempt got:</p>
     * <ul>
     *   <li><b>Attached to the target</b> &rarr; the swap already completed (the new disk is only attached in step 4,
     *       after a good create), so this is {@link ResumeAction#CLEANUP_ONLY}. An attached disk is never deleted, so a
     *       completed migration's data is never destroyed.</li>
     *   <li><b>Attached to a different instance</b> &rarr; not ours to touch, fail fast
     *       ({@link CloudbreakServiceException}).</li>
     *   <li><b>Unattached</b> &rarr; the adoption guard applies: the disk is only resumed when it is READY <b>and</b>
     *       was created from our deterministic snapshot ({@code expectedSnapshotName} equals the last path segment of
     *       its {@code sourceSnapshot}). Otherwise it is an untrustworthy leftover of a failed attempt and, being
     *       unattached, is safely self-healed via {@link ResumeAction#RECREATE_NEW_DISK} (delete it, then full
     *       migration).</li>
     * </ul>
     *
     * <p>Retries on transient GCP errors so a rerun's resume decision does not fail on a flaky read.</p>
     *
     * @param newDiskParams        the target-type disk this migration creates
     * @param oldDiskParams        the currently attached source disk being migrated
     * @param expectedSnapshotName the deterministic snapshot name the new disk must have been created from
     */
    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public DiskTypeChangeResumePoint resolveDiskTypeChangeResumePoint(GcpDiskAttachmentParameters newDiskParams,
            GcpDiskAttachmentParameters oldDiskParams, String expectedSnapshotName) {
        String newDiskName = newDiskParams.diskName();
        Optional<Disk> newDisk = getDisk(newDiskParams);
        if (newDisk.isEmpty()) {
            LOGGER.info("New GCP disk '{}' does not exist yet; running the full disk-type migration.", newDiskName);
            return new DiskTypeChangeResumePoint(ResumeAction.FULL_MIGRATION, Optional.empty());
        }
        Disk disk = newDisk.get();
        List<String> users = disk.getUsers();
        if (attachedToTarget(users, newDiskParams.instanceId())) {
            LOGGER.info("New GCP disk '{}' already exists and is attached to the target instance '{}'; a prior attempt completed the swap, "
                    + "only cleanup remains.", newDiskName, newDiskParams.instanceId());
            return new DiskTypeChangeResumePoint(ResumeAction.CLEANUP_ONLY, Optional.of(disk));
        }
        if (users != null && !users.isEmpty()) {
            throw new CloudbreakServiceException(String.format("GCP disk '%s' already exists and is attached to a different instance %s than the "
                    + "target '%s'; refusing to adopt it during the disk-type change.", newDiskName, users, newDiskParams.instanceId()));
        }
        // The new disk is unattached: adopt it only when it is READY and provably created from our snapshot; otherwise
        // it is an untrustworthy leftover and, since nothing is serving data off it, is safe to delete and recreate.
        if (!DISK_STATUS_READY.equals(disk.getStatus())) {
            LOGGER.info("New GCP disk '{}' exists but is not READY (status: {}); deleting the unusable leftover and re-running the full migration.",
                    newDiskName, disk.getStatus());
            return new DiskTypeChangeResumePoint(ResumeAction.RECREATE_NEW_DISK, Optional.of(disk));
        }
        if (!createdFromExpectedSnapshot(disk, expectedSnapshotName)) {
            LOGGER.info("New GCP disk '{}' is unattached but was not created from the expected snapshot '{}' (sourceSnapshot: {}); "
                    + "deleting the untrustworthy leftover and re-running the full migration.", newDiskName, expectedSnapshotName, disk.getSourceSnapshot());
            return new DiskTypeChangeResumePoint(ResumeAction.RECREATE_NEW_DISK, Optional.of(disk));
        }
        // The new disk is READY, ours and unattached: the only question left is whether the old disk still needs detaching.
        if (oldDiskAttachedToTarget(oldDiskParams)) {
            LOGGER.info("New GCP disk '{}' is READY and unattached while the old disk '{}' is still attached to instance '{}'; resuming at the detach step.",
                    newDiskName, oldDiskParams.diskName(), oldDiskParams.instanceId());
            return new DiskTypeChangeResumePoint(ResumeAction.RESUME_AT_DETACH, Optional.of(disk));
        }
        LOGGER.info("New GCP disk '{}' is READY and unattached and the old disk '{}' is already detached; resuming at the attach step.",
                newDiskName, oldDiskParams.diskName());
        return new DiskTypeChangeResumePoint(ResumeAction.RESUME_AT_ATTACH, Optional.of(disk));
    }

    private Optional<Disk> getDisk(GcpDiskAttachmentParameters parameters) {
        String diskName = parameters.diskName();
        try {
            return Optional.of(parameters.compute().disks().get(parameters.projectId(), parameters.zone(), diskName).execute());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.debug("GCP disk '{}' not found on provider.", diskName);
                return Optional.empty();
            }
            throw classifyGoogleException(e, "get", diskName);
        } catch (IOException e) {
            throw new CloudConnectorException(String.format("Failed to read the status of GCP disk '%s', retrying.", diskName), e);
        }
    }

    private boolean createdFromExpectedSnapshot(Disk disk, String expectedSnapshotName) {
        String sourceSnapshot = disk.getSourceSnapshot();
        return sourceSnapshot != null && expectedSnapshotName.equals(lastSnapshotSegment(sourceSnapshot));
    }

    private String lastSnapshotSegment(String sourceSnapshot) {
        int index = sourceSnapshot.lastIndexOf('/');
        return index >= 0 ? sourceSnapshot.substring(index + 1) : sourceSnapshot;
    }

    private boolean attachedToTarget(List<String> users, String instanceId) {
        return users != null && users.stream().anyMatch(user -> instanceId.equals(lastPathSegment(user)));
    }

    private boolean oldDiskAttachedToTarget(GcpDiskAttachmentParameters oldDiskParams) {
        return getDisk(oldDiskParams).map(disk -> attachedToTarget(disk.getUsers(), oldDiskParams.instanceId())).orElse(false);
    }

    private boolean attachedToTargetInstance(GcpDiskAttachmentParameters parameters) throws Exception {
        try {
            Disk disk = parameters.compute().disks().get(parameters.projectId(), parameters.zone(), parameters.diskName()).execute();
            List<String> users = disk.getUsers();
            if (users == null || users.isEmpty()) {
                return false;
            }
            boolean attachedToTarget = users.stream().anyMatch(user -> parameters.instanceId().equals(lastPathSegment(user)));
            if (!attachedToTarget) {
                LOGGER.warn("Disk '{}' is attached to a different instance {} than the detach target '{}', skipping detach.",
                        parameters.diskName(), users, parameters.instanceId());
            }
            return attachedToTarget;
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("Disk '{}' not found on provider, treating as already detached.", parameters.diskName());
                return false;
            }
            throw e;
        }
    }

    /**
     * Polls the given operation-aware resources to completion in a single scheduler pass, after all inserts/deletes
     * have been submitted. Returns immediately when there is nothing to poll (every disk was reused or already gone).
     */
    public List<CloudResourceStatus> pollDiskOperations(AuthenticatedContext auth, GcpContext gcpContext, List<CloudResource> operationAwareResources)
            throws Exception {
        if (operationAwareResources.isEmpty()) {
            return List.of();
        }
        return waitForOperation(auth, gcpContext, operationAwareResources);
    }

    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(ResourceType.GCP_ATTACHED_DISKSET, context, auth, resources);
    }

    /**
     * A disk with this name already exists on the provider. It is safe to reuse (skip the insert and the poll) when it
     * is unattached or attached only to the target instance of this add-volumes request; otherwise a same-named disk in
     * use by another instance is unexpected and we fail so it is never silently adopted.
     */
    private void verifyReusable(Disk existingDisk, GcpCreateDiskParameters params) {
        String targetInstanceId = params.cloudResource().getInstanceId();
        if (attachedOnlyToTargetInstance(existingDisk, targetInstanceId)) {
            LOGGER.info("Reusing already existing GCP disk {} in zone {} for instance {}.", params.diskName(), params.zone(), targetInstanceId);
            return;
        }
        throw new CloudbreakServiceException(String.format("GCP disk %s already exists and is attached to a different instance (users: %s) "
                + "than the add-volumes target %s.", params.diskName(), existingDisk.getUsers(), targetInstanceId));
    }

    private boolean attachedOnlyToTargetInstance(Disk existingDisk, String targetInstanceId) {
        List<String> users = existingDisk.getUsers();
        if (users == null || users.isEmpty()) {
            return true;
        }
        return users.stream().allMatch(user -> lastPathSegment(user).equals(targetInstanceId));
    }

    private String lastPathSegment(String user) {
        int index = user.lastIndexOf(GcpConstants.INSTANCE_URL_SEGMENT);
        return index >= 0 ? user.substring(index + GcpConstants.INSTANCE_URL_SEGMENT.length()) : user;
    }

    private RuntimeException classifyGoogleException(GoogleJsonResponseException e, String action, String diskName) {
        int statusCode = e.getStatusCode();
        String message = String.format("Failed to %s GCP disk %s: %s", action, diskName, e.getMessage());
        if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value() || statusCode >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            LOGGER.warn("Transient GCP error ({}) while trying to {} disk {}, retrying.", statusCode, action, diskName, e);
            return new CloudConnectorException(message, e);
        }
        LOGGER.warn("Non-transient GCP error ({}) while trying to {} disk {}, failing fast.", statusCode, action, diskName, e);
        return new CloudbreakServiceException(message, e);
    }

    private List<CloudResourceStatus> waitForOperation(AuthenticatedContext authenticatedContext, GcpContext context, List<CloudResource> resources)
            throws Exception {
        PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(this, authenticatedContext, resources, context, true);
        return syncPollingScheduler.schedule(task);
    }
}
