package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.Set;

public class AddAmbariHostsSuccess {

    private Long clusterId;
    private Set<String> hostNames;

    public AddAmbariHostsSuccess(Long clusterId, Set<String> hostNames) {
        this.clusterId = clusterId;
        this.hostNames = hostNames;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public void setHostNames(Set<String> hostNames) {
        this.hostNames = hostNames;
    }
}
