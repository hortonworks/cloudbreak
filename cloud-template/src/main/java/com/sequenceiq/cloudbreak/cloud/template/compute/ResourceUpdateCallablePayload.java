package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class ResourceUpdateCallablePayload {

    private final List<CloudInstance> instances;

    private final Group group;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudStack cloudStack;

    public ResourceUpdateCallablePayload(List<CloudInstance> instances, Group group,
        ResourceBuilderContext context, AuthenticatedContext auth, CloudStack cloudStack) {
        this.instances = instances;
        this.group = group;
        this.context = context;
        this.auth = auth;
        this.cloudStack = cloudStack;
    }

    public List<CloudInstance> getInstances() {
        return instances;
    }

    public Group getGroup() {
        return group;
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

}
