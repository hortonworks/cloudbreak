package com.sequenceiq.cloudbreak.cloud.gcp.service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record GcpResizeDiskParameters(
        Compute compute,
        String projectId,
        String preferredZone,
        String diskName,
        int size,
        CloudResource cloudResource,
        AuthenticatedContext authenticatedContext) {
}
