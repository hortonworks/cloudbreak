package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterManagerRequest extends AbstractClusterScaleRequest {

    private final Map<String, Integer> hostGroupWithAdjustment;

    private final boolean primaryGatewayChanged;

    private final boolean repair;

    @JsonCreator
    public UpscaleClusterManagerRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("primaryGatewayChanged") boolean primaryGatewayChanged,
            @JsonProperty("repair") boolean repair) {
        super(stackId, hostGroupWithAdjustment.keySet());
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.primaryGatewayChanged = primaryGatewayChanged;
        this.repair = repair;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }

    public boolean isPrimaryGatewayChanged() {
        return primaryGatewayChanged;
    }

    public boolean isRepair() {
        return repair;
    }

    @JsonIgnore
    @Override
    public Set<String> getHostGroupNames() {
        return super.getHostGroupNames();
    }
}
