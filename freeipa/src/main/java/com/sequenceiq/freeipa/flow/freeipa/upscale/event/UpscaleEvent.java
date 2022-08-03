package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpscaleEvent extends StackEvent {

    private final Integer instanceCountByGroup;

    private final Boolean repair;

    private final String operationId;

    private final boolean chained;

    private final boolean finalChain;

    @JsonCreator
    public UpscaleEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceCountByGroup") Integer instanceCountByGroup,
            @JsonProperty("repair") Boolean repair,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalChain") boolean finalChain,
            @JsonProperty("operationId") String operationId) {
        super(selector, stackId);
        this.instanceCountByGroup = instanceCountByGroup;
        this.repair = repair;
        this.chained = chained;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    public Integer getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public Boolean getRepair() {
        return repair;
    }

    public String getOperationId() {
        return operationId;
    }

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalChain() {
        return finalChain;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(UpscaleEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && Objects.equals(instanceCountByGroup, event.instanceCountByGroup)
                        && Objects.equals(repair, event.repair)
                        && finalChain == event.finalChain
                        && chained == event.chained);
    }

    @Override
    public String toString() {
        return "UpscaleEvent{" +
                "instanceCountByGroup=" + instanceCountByGroup +
                ", repair=" + repair +
                ", operationId='" + operationId + '\'' +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                "} " + super.toString();
    }
}
