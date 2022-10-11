package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterRequest extends AbstractClusterScaleRequest {

    private final boolean repair;

    private final boolean restartServices;

    private final Map<String, Set<String>> hostGroupsWithHostNames;

    private final Map<String, Integer> hostGroupWithAdjustment;

    private final boolean primaryGatewayChanged;

    public UpscaleClusterRequest(Long stackId, Set<String> hostGroups, boolean repair, boolean restartServices, Map<String, Integer> hostGroupWithAdjustment,
            boolean primaryGatewayChanged) {
        super(stackId, hostGroups);
        this.repair = repair;
        this.restartServices = restartServices;
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.hostGroupsWithHostNames = new HashMap<>();
        this.primaryGatewayChanged = primaryGatewayChanged;
    }

    @JsonCreator
    public UpscaleClusterRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("repair") boolean repair,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonProperty("hostGroupsWithHostNames") Map<String, Set<String>> hostGroupsWithHostNames,
            @JsonProperty("hostGroupWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("primaryGatewayChanged") boolean primaryGatewayChanged) {
        super(stackId, hostGroups);
        this.repair = repair;
        this.restartServices = restartServices;
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames;
        this.primaryGatewayChanged = primaryGatewayChanged;
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

    public boolean isPrimaryGatewayChanged() {
        return primaryGatewayChanged;
    }
}
