package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RollingVerticalScaleResizeResult extends StackEvent {

    @JsonCreator
    public RollingVerticalScaleResizeResult(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "RollingVerticalScaleResizeResult{} " + super.toString();
    }
}
