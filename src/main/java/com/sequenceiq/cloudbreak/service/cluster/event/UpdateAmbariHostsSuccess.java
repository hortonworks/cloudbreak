package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.Set;

public class UpdateAmbariHostsSuccess {

    private Long clusterId;
    private Set<String> hostNames;
    private boolean decommision;

    public UpdateAmbariHostsSuccess(Long clusterId, Set<String> hostNames, boolean decommision) {
        this.clusterId = clusterId;
        this.hostNames = hostNames;
        this.decommision = decommision;
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

    public boolean isDecommision() {
        return decommision;
    }

    public void setDecommision(boolean decommision) {
        this.decommision = decommision;
    }
}
