package com.sequenceiq.cloudbreak.cloud.gcp.service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

/**
 * Parameters describing a single snapshot create/delete operation used by the GCP disk-type-change flow.
 *
 * <p>{@code sourceDiskName} is the zonal disk the snapshot is taken from (only used by create); {@code snapshotName}
 * is the global snapshot resource name (used by both create and delete). Snapshot create runs as a <b>zonal</b>
 * operation (it targets a zonal disk) while snapshot delete runs as a <b>global</b> operation; the operation poller
 * derives the scope from the returned {@link com.google.api.services.compute.model.Operation} automatically.</p>
 */
public record GcpSnapshotParameters(
        Compute compute,
        String projectId,
        String zone,
        String sourceDiskName,
        String snapshotName,
        CloudResource cloudResource,
        AuthenticatedContext authenticatedContext) {
}
