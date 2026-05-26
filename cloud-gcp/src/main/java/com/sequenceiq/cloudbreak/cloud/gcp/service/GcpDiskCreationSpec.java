package com.sequenceiq.cloudbreak.cloud.gcp.service;

import com.google.api.services.compute.model.Disk;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;

/**
 * Describes a single GCP data disk to be created: the {@link Disk} to insert, the zone to create it in, the
 * disk-set {@link CloudResource} it belongs to, and the {@link VolumeSetAttributes.Volume} that should be added to
 * that resource once the disk has been created successfully.
 */
public record GcpDiskCreationSpec(
        CloudResource resource,
        VolumeSetAttributes.Volume volume,
        Disk disk,
        String zone) {
}
