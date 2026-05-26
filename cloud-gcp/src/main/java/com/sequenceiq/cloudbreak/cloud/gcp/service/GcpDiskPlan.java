package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.util.List;

/**
 * The result of planning an add-volumes request for a group: the fresh disks that still need to be created
 * ({@code toCreate}) and the orphaned disks reclaimed from a previous attempt that are reused as-is
 * ({@code reused}). Together they satisfy the requested volume count per instance.
 */
public record GcpDiskPlan(
        List<GcpDiskCreationSpec> toCreate,
        List<GcpReusedDisk> reused) {
}
