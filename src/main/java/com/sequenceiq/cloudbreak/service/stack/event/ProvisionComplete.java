package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;

public class ProvisionComplete extends ProvisionEvent {

    private Set<Resource> resources;

    public ProvisionComplete(CloudPlatform cloudPlatform, Long stackId, Set<Resource> resources) {
        super(cloudPlatform, stackId);
        this.resources = resources;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

}
