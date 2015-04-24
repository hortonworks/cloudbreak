package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.Resource;

public abstract class CreateResourceRequest {

    private List<Resource> buildableResources = new ArrayList<>();

    public CreateResourceRequest(List<Resource> buildableResources) {
        this.buildableResources = buildableResources;
    }

    public List<Resource> getBuildableResources() {
        return buildableResources;
    }
}
