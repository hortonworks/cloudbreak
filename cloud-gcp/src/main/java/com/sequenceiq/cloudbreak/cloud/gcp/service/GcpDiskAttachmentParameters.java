package com.sequenceiq.cloudbreak.cloud.gcp.service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

/**
 * Parameters describing a single disk attach/detach/delete operation against a GCP instance.
 *
 * <p>{@code diskName} and {@code deviceName} are two distinct GCP concepts that is intentionally kept separate:
 * <ul>
 *   <li>{@code diskName} — the GCP <b>disk resource</b> name, used to address the disk itself
 *       ({@code disks().get(...)}, {@code disks().delete(...)} and the {@code source} URL of an
 *       {@link com.google.api.services.compute.model.AttachedDisk}).</li>
 *   <li>{@code deviceName} — the <b>attachment device name</b> under which the disk is exposed on the instance,
 *       required by {@code instances().detachDisk(...)}.</li>
 * </ul>
 * They currently hold the same value (the short disk name, {@code Volume#getId()}) because attach sets the device
 * name to the disk id to match {@code GcpInstanceResourceBuilder#createDisk}; the fields are kept separate so a future
 * divergence (e.g. a custom device name) does not silently break detach. {@code Volume#getDevice()} must NOT be used
 * for either: it holds the OS {@code /dev/disk/by-id/google-<id>} path, not a provider-side name.</p>
 */
public record GcpDiskAttachmentParameters(
        Compute compute,
        String projectId,
        String zone,
        String instanceId,
        String diskName,
        String deviceName,
        CloudResource cloudResource,
        AuthenticatedContext authenticatedContext) {
}
