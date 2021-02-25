package com.sequenceiq.cloudbreak.cloud.template.compute;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class ResourceDeletionCallablePayload {

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudResource resource;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    private final boolean cancellable;

    public ResourceDeletionCallablePayload(ResourceBuilderContext context, AuthenticatedContext auth, CloudResource resource,
            ComputeResourceBuilder<ResourceBuilderContext> builder, boolean cancellable) {
        this.context = context;
        this.auth = auth;
        this.resource = resource;
        this.builder = builder;
        this.cancellable = cancellable;
    }

    public ResourceBuilderContext getContext() {
        return context;
    }

    public AuthenticatedContext getAuth() {
        return auth;
    }

    public CloudResource getResource() {
        return resource;
    }

    public ComputeResourceBuilder<ResourceBuilderContext> getBuilder() {
        return builder;
    }

    public boolean isCancellable() {
        return cancellable;
    }
}
