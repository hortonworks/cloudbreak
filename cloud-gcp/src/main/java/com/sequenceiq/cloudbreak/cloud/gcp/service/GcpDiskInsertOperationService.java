package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpExceptionUtil;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Shared existence-check-before-insert core for a single GCP disk. Both the reattach build path
 * ({@code GcpAttachedDiskResourceBuilder}) and the add-volumes create path
 * ({@link GcpDiskUpdateRetryService}) go through this so there is a single, idempotent GCP disk-insert code path.
 */
@Service
public class GcpDiskInsertOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDiskInsertOperationService.class);

    /**
     * Fetches the disk by name and, only if it is absent, inserts it. Returns the insert {@link Operation} when a new
     * disk was created, or the pre-existing {@link Disk} when one already existed so the caller can decide whether to
     * reuse it. Missing disks (HTTP 404/403) are treated as absent; other fetch failures are surfaced as
     * {@link GcpResourceException}.
     */
    public GcpDiskInsertOutcome insertDiskIfAbsent(Compute compute, String projectId, String zone, Disk disk, String resourceName)
            throws IOException {
        Optional<Disk> existingDisk = fetchDisk(compute, projectId, zone, disk.getName(), resourceName);
        if (existingDisk.isPresent()) {
            LOGGER.info("GCP disk '{}' already exists, using it.", disk.getName());
            return new GcpDiskInsertOutcome(Optional.empty(), existingDisk);
        }
        LOGGER.info("Inserting GCP disk {} in zone {}.", disk.getName(), zone);
        Operation insertOperation;
        try {
            insertOperation = compute.disks().insert(projectId, zone, disk).execute();
        } catch (GoogleJsonResponseException e) {
            if (GcpExceptionUtil.conflictException(e)) {
                // Another creator (e.g. a concurrent or retried flow) won the race between the fetch above and this
                // insert. Re-fetch and reuse the disk so the create stays idempotent instead of failing on the 409.
                LOGGER.info("GCP disk '{}' was created concurrently (409), re-fetching and reusing it.", disk.getName());
                Optional<Disk> conflictingDisk = fetchDisk(compute, projectId, zone, disk.getName(), resourceName);
                if (conflictingDisk.isPresent()) {
                    return new GcpDiskInsertOutcome(Optional.empty(), conflictingDisk);
                }
            }
            throw e;
        }
        if (insertOperation.getHttpErrorStatusCode() != null) {
            throw new GcpResourceException(String.format("Create request for GCP disk %s failed: %s",
                    disk.getName(), insertOperation.getHttpErrorMessage()), ResourceType.GCP_ATTACHED_DISKSET, disk.getName());
        }
        return new GcpDiskInsertOutcome(Optional.of(insertOperation), Optional.empty());
    }

    private Optional<Disk> fetchDisk(Compute compute, String projectId, String zone, String diskName, String resourceName) throws IOException {
        try {
            return Optional.ofNullable(compute.disks().get(projectId, zone, diskName).execute());
        } catch (GoogleJsonResponseException e) {
            if (GcpExceptionUtil.resourceNotFoundException(e)) {
                LOGGER.debug("GCP disk {} not found on provider.", diskName);
                return Optional.empty();
            }
            throw new GcpResourceException("Fetching GCP disk from provider failed", ResourceType.GCP_ATTACHED_DISKSET, resourceName, e);
        }
    }

    /**
     * Result of an insert-if-absent call: {@code operation} is present when a fresh insert was submitted,
     * {@code existingDisk} is present when a disk with the same name already existed on the provider (no insert done).
     * Exactly one of the two is present.
     */
    public record GcpDiskInsertOutcome(Optional<Operation> operation, Optional<Disk> existingDisk) {
    }
}
