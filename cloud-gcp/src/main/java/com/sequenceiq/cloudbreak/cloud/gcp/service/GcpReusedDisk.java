package com.sequenceiq.cloudbreak.cloud.gcp.service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;

/**
 * An orphaned GCP disk from a previous (failed/rerun) add-volumes attempt that already exists unattached on the
 * provider and is reclaimed instead of creating a fresh disk. No cloud call is needed to reuse it; the
 * {@link VolumeSetAttributes.Volume} is recorded on the {@link CloudResource}'s volume set on success.
 */
public record GcpReusedDisk(
        CloudResource resource,
        VolumeSetAttributes.Volume volume) {
}
