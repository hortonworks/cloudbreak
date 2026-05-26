package com.sequenceiq.cloudbreak.cloud.gcp.service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record GcpCreateDiskParameters(
        Compute compute,
        String projectId,
        String zone,
        String diskName,
        Disk disk,
        CloudResource cloudResource,
        AuthenticatedContext authenticatedContext) {
}
