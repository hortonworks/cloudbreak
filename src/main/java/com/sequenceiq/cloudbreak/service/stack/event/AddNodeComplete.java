package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;

public class AddNodeComplete extends ProvisionEvent {
    private Set<Resource> resources;
    private String hostgroup;

    public AddNodeComplete(CloudPlatform cloudPlatform, Long stackId, Set<Resource> resources, String hostgroup) {
        super(cloudPlatform, stackId);
        this.resources = resources;
        this.hostgroup = hostgroup;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public String getHostgroup() {
        return hostgroup;
    }

    public void setHostgroup(String hostgroup) {
        this.hostgroup = hostgroup;
    }
}
