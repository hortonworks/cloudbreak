package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class ResourceUpdateCallablePayload {

    private final CloudResource resource;

    private final CloudInstance cloudInstance;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudStack cloudStack;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    private final Optional<String> targetGroup;

    public ResourceUpdateCallablePayload(CloudResource resource, CloudInstance cloudInstance, ResourceBuilderContext context,
        AuthenticatedContext auth, CloudStack cloudStack, ComputeResourceBuilder<ResourceBuilderContext> builder, Optional<String> targetGroup) {
        this.resource = resource;
        this.cloudInstance = cloudInstance;
        this.context = context;
        this.auth = auth;
        this.cloudStack = cloudStack;
        this.builder = builder;
        this.targetGroup = targetGroup;
    }

    public CloudResource getCloudResource() {
        return resource;
    }

    public ResourceBuilderContext getContext() {
        return context;
    }

    public AuthenticatedContext getAuth() {
        return auth;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public CloudInstance getCloudInstance() {
        return cloudInstance;
    }

    public ComputeResourceBuilder<ResourceBuilderContext> getBuilder() {
        return builder;
    }

    public Optional<String> getTargetGroup() {
        return targetGroup;
    }
}
