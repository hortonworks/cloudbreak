package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;

public class AddInstancesComplete extends ProvisionEvent {

    private Set<Resource> resources;
    private String instanceGroup;

    public AddInstancesComplete(CloudPlatform cloudPlatform, Long stackId, Set<Resource> resources, String instanceGroup) {
        super(cloudPlatform, stackId);
        this.resources = resources;
        this.instanceGroup = instanceGroup;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }
}
