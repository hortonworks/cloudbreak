package com.sequenceiq.cloudbreak.cloud.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class CloudResponse {

    private StackContext stackContext;

    private Set<CloudResource> resources;

    public CloudResponse(StackContext stackContext, Set<CloudResource> resources) {
        this.stackContext = stackContext;
        this.resources = resources;
    }

    public StackContext getStackContext() {
        return stackContext;
    }

    public Set<CloudResource> getResources() {
        return resources;
    }
}


