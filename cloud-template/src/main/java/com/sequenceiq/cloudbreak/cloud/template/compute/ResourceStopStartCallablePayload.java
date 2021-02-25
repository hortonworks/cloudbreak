package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class ResourceStopStartCallablePayload {

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final List<CloudInstance> instances;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    public ResourceStopStartCallablePayload(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudInstance> instances, ComputeResourceBuilder<ResourceBuilderContext> builder) {
        this.context = context;
        this.auth = auth;
        this.instances = instances;
        this.builder = builder;
    }

    public ResourceBuilderContext getContext() {
        return context;
    }

    public AuthenticatedContext getAuth() {
        return auth;
    }

    public List<CloudInstance> getInstances() {
        return instances;
    }

    public ComputeResourceBuilder<ResourceBuilderContext> getBuilder() {
        return builder;
    }
}
