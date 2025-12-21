package com.sequenceiq.cloudbreak.cloud.azure.resource.domain;

import com.azure.resourcemanager.compute.models.Disk;

public record AzureDiskWithLun(
        Disk disk,
        int lun) {
}
