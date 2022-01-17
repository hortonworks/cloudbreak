package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Map;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterManagerRequest extends AbstractClusterScaleRequest {

    private final Map<String, Integer> hostGroupWithAdjustment;

    private final boolean primaryGatewayChanged;

    private final boolean repair;

    public UpscaleClusterManagerRequest(Long stackId, Map<String, Integer> hostGroupWithAdjustment, boolean primaryGatewayChanged, boolean repair) {
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

}
