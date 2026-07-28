package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RollingVerticalScaleDescribeRequest extends StackEvent {

    private final String instanceId;

    private final FreeIpaVerticalScaleParameters scaleConfig;

    @JsonCreator
    public RollingVerticalScaleDescribeRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("instanceId") String instanceId,
            @JsonProperty("scaleConfig") FreeIpaVerticalScaleParameters scaleConfig) {
        super(resourceId);
        this.instanceId = instanceId;
        this.scaleConfig = scaleConfig;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public FreeIpaVerticalScaleParameters getScaleConfig() {
        return scaleConfig;
    }

    @Override
    public String toString() {
        return "RollingVerticalScaleDescribeRequest{instanceId='" + instanceId + "', scaleConfig=" + scaleConfig + "} " + super.toString();
    }
}
