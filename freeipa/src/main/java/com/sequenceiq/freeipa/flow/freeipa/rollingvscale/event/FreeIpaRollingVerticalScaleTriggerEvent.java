package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaRollingVerticalScaleTriggerEvent extends StackEvent {

    private final String instanceId;

    private final FreeIpaVerticalScaleParameters scaleConfig;

    private final boolean finalChain;

    private final String operationId;

    @JsonCreator
    public FreeIpaRollingVerticalScaleTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceId") String instanceId,
            @JsonProperty("scaleConfig") FreeIpaVerticalScaleParameters scaleConfig,
            @JsonProperty("finalChain") boolean finalChain,
            @JsonProperty("operationId") String operationId) {
        super(FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_TRIGGER_EVENT.event(), stackId);
        this.instanceId = instanceId;
        this.scaleConfig = scaleConfig;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    public FreeIpaRollingVerticalScaleTriggerEvent(Long stackId, String instanceId, FreeIpaVerticalScaleParameters scaleConfig) {
        this(stackId, instanceId, scaleConfig, false, null);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public FreeIpaVerticalScaleParameters getScaleConfig() {
        return scaleConfig;
    }

    public boolean isFinalChain() {
        return finalChain;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "FreeIpaRollingVerticalScaleTriggerEvent{" +
                "instanceId='" + instanceId + '\'' +
                ", scaleConfig=" + scaleConfig +
                ", finalChain=" + finalChain +
                ", operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
