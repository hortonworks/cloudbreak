package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

public class MetadataUpdateComplete extends ProvisionEvent {
    private Set<CoreInstanceMetaData> coreInstanceMetaData = new HashSet<>();
    private String hostGroup;

    public MetadataUpdateComplete(CloudPlatform cloudPlatform, Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData, String hostGroup) {
        super(cloudPlatform, stackId);
        this.coreInstanceMetaData = coreInstanceMetaData;
        this.hostGroup = hostGroup;
    }

    public Set<CoreInstanceMetaData> getCoreInstanceMetaData() {
        return coreInstanceMetaData;
    }

    public void setCoreInstanceMetaData(Set<CoreInstanceMetaData> coreInstanceMetaData) {
        this.coreInstanceMetaData = coreInstanceMetaData;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }
}
