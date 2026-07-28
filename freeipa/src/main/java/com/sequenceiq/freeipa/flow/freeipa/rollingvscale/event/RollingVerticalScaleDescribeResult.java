package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RollingVerticalScaleDescribeResult extends StackEvent {

    private final FreeIpaVerticalScaleParameters scaleConfig;

    @JsonCreator
    public RollingVerticalScaleDescribeResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("scaleConfig") FreeIpaVerticalScaleParameters scaleConfig) {
        super(resourceId);
        this.scaleConfig = scaleConfig;
    }

    public FreeIpaVerticalScaleParameters getScaleConfig() {
        return scaleConfig;
    }

    @Override
    public String toString() {
        return "RollingVerticalScaleDescribeResult{scaleConfig=" + scaleConfig + "} " + super.toString();
    }
}
