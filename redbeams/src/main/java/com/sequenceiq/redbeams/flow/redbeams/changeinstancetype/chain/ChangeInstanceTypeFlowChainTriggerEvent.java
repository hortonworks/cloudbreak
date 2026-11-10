package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.chain;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class ChangeInstanceTypeFlowChainTriggerEvent extends RedbeamsEvent {

    private final String targetInstanceType;

    @JsonCreator
    public ChangeInstanceTypeFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("targetInstanceType") String targetInstanceType) {
        super(selector, resourceId);
        this.targetInstanceType = targetInstanceType;
    }

    public String getTargetInstanceType() {
        return targetInstanceType;
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(ChangeInstanceTypeFlowChainTriggerEvent.class, other,
                event -> Objects.equals(targetInstanceType, event.targetInstanceType));
    }

    @Override
    public String toString() {
        return "ChangeInstanceTypeFlowChainTriggerEvent{" +
                "targetInstanceType='" + targetInstanceType + '\'' +
                "} " + super.toString();
    }
}
