package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaRollingVerticalScaleChainTriggerEvent extends StackEvent {

    private final FreeIpaVerticalScaleParameters scaleConfig;

    private final String operationId;

    @JsonCreator
    public FreeIpaRollingVerticalScaleChainTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("scaleConfig") FreeIpaVerticalScaleParameters scaleConfig,
            @JsonProperty("operationId") String operationId) {
        super(FlowChainTriggers.FREEIPA_ROLLING_VERTICAL_SCALE_CHAIN_TRIGGER_EVENT, stackId);
        this.scaleConfig = scaleConfig;
        this.operationId = operationId;
    }

    public FreeIpaVerticalScaleParameters getScaleConfig() {
        return scaleConfig;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "FreeIpaRollingVerticalScaleChainTriggerEvent{" +
                "scaleConfig=" + scaleConfig +
                ", operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
