package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.Set;

public class UpdateAmbariHostsSuccess {

    private Long clusterId;
    private Set<String> hostNames;
    private boolean decommission;

    public UpdateAmbariHostsSuccess(Long clusterId, Set<String> hostNames, boolean decommission) {
        this.clusterId = clusterId;
        this.hostNames = hostNames;
        this.decommission = decommission;
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

    public boolean isDecommission() {
        return decommission;
    }

    public void setDecommission(boolean decommission) {
        this.decommission = decommission;
    }
}
