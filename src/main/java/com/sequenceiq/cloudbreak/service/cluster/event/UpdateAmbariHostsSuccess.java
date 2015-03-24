package com.sequenceiq.cloudbreak.service.cluster.event;

import java.util.Set;

public class UpdateAmbariHostsSuccess {

    private Long clusterId;
    private Set<String> hostNames;
    private boolean decommission;
    private Boolean withStackUpdate;
    private String hostGroup;

    public UpdateAmbariHostsSuccess(Long clusterId, Set<String> hostNames, boolean decommission, String hostGroup, Boolean withStackUpdate) {
        this.clusterId = clusterId;
        this.hostNames = hostNames;
        this.decommission = decommission;
        this.withStackUpdate = withStackUpdate;
        this.hostGroup = hostGroup;
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

    public boolean isDecommission() {
        return decommission;
    }

    public Boolean isWithStackUpdate() {
        return withStackUpdate;
    }

    public Boolean getWithStackUpdate() {
        return withStackUpdate;
    }

    public String getHostGroup() {
        return hostGroup;
    }
}
