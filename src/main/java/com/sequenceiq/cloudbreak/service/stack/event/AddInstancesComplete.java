package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;

public class AddInstancesComplete extends ProvisionEvent {

    private Set<Resource> resources;
    private String instanceGroup;
    private Boolean withStackUpdate;

    public AddInstancesComplete(CloudPlatform cloudPlatform, Long stackId, Set<Resource> resources, String instanceGroup,
            Boolean withStackUpdate) {
        super(cloudPlatform, stackId);
        this.resources = resources;
        this.instanceGroup = instanceGroup;
        this.withStackUpdate = withStackUpdate;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public Boolean isWithStackUpdate() {
        return withStackUpdate;
    }
}
