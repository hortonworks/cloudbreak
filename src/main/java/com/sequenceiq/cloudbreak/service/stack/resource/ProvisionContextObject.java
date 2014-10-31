package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;

public abstract class ProvisionContextObject {

    private Long stackId;

    private List<Resource> networkResources = new ArrayList<>();

    protected ProvisionContextObject(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public List<Resource> getNetworkResources() {
        return networkResources;
    }

    public List<Resource> filterResourcesByType(ResourceType resourceType) {
        List<Resource> resourcesTemp = new ArrayList<>();
        for (Resource resource : networkResources) {
            if (resourceType.equals(resource.getResourceType())) {
                resourcesTemp.add(resource);
            }
        }
        return resourcesTemp;
    }
}
