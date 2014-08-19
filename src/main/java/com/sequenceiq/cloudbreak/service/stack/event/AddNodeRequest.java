package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class AddNodeRequest extends ProvisionEvent {
    private String hostgroup;
    public AddNodeRequest(CloudPlatform cloudPlatform, Long stackId, String hostgroup) {
        super(cloudPlatform, stackId);
        this.hostgroup = hostgroup;
    }

    public String getHostgroup() {
        return hostgroup;
    }

    public void setHostgroup(String hostgroup) {
        this.hostgroup = hostgroup;
    }
}
