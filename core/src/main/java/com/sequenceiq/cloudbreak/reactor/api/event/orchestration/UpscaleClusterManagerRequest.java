package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterManagerRequest extends AbstractClusterScaleRequest {

    private final Integer scalingAdjustment;

    private final boolean primaryGatewayChanged;

    private final boolean repair;

    public UpscaleClusterManagerRequest(Long stackId, String hostGroupName, Integer scalingAdjustment, boolean primaryGatewayChanged, boolean repair) {
        super(stackId, hostGroupName);
        this.scalingAdjustment = scalingAdjustment;
        this.primaryGatewayChanged = primaryGatewayChanged;
        this.repair = repair;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public boolean isPrimaryGatewayChanged() {
        return primaryGatewayChanged;
    }

    public boolean isRepair() {
        return repair;
    }

}
