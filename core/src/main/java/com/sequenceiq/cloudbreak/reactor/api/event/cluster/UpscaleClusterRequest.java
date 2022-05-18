package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterRequest extends AbstractClusterScaleRequest {

    private boolean repair;

    private boolean restartServices;

    private Map<String, Set<String>> hostGroupsWithHostNames;

    private Map<String, Integer> hostGroupWithAdjustment;

    public UpscaleClusterRequest(Long stackId, Set<String> hostGroups, boolean repair, boolean restartServices, Map<String, Integer> hostGroupWithAdjustment) {
        super(stackId, hostGroups);
        this.repair = repair;
        this.restartServices = restartServices;
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.hostGroupsWithHostNames = new HashMap<>();
    }

    public UpscaleClusterRequest(Long stackId, Set<String> hostGroups, boolean repair, boolean restartServices,
            Map<String, Set<String>> hostGroupsWithHostNames, Map<String, Integer> hostGroupWithAdjustment) {
        super(stackId, hostGroups);
        this.repair = repair;
        this.restartServices = restartServices;
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames;
    }

    public boolean isRepair() {
        return repair;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public Map<String, Set<String>> getHostGroupsWithHostNames() {
        return hostGroupsWithHostNames;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }
}
