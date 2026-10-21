package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class ResourceBulkDeletionCallablePayload {

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final List<CloudResourceStatus> deletionStatuses;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    private final boolean cancellable;

    public ResourceBulkDeletionCallablePayload(ResourceBuilderContext context, AuthenticatedContext auth, List<CloudResourceStatus> deletionStatuses,
            ComputeResourceBuilder<ResourceBuilderContext> builder, boolean cancellable) {
        this.context = context;
        this.auth = auth;
        this.deletionStatuses = deletionStatuses;
        this.builder = builder;
        this.cancellable = cancellable;
    }

    public ResourceBuilderContext getContext() {
        return context;
    }

    public AuthenticatedContext getAuth() {
        return auth;
    }

    public List<CloudResourceStatus> getDeletionStatuses() {
        return deletionStatuses;
    }

    public ComputeResourceBuilder<ResourceBuilderContext> getBuilder() {
        return builder;
    }

    public boolean isCancellable() {
        return cancellable;
    }
}
