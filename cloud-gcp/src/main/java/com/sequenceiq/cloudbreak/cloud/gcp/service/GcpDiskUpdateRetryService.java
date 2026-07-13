package com.sequenceiq.cloudbreak.cloud.gcp.service;

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
