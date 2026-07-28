package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaRollingVerticalScaleFailureEvent extends StackFailureEvent {

    @JsonCreator
    public FreeIpaRollingVerticalScaleFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FAILURE_EVENT.event(), stackId, exception, FailureType.ERROR);
    }

    @Override
    public String toString() {
        return "FreeIpaRollingVerticalScaleFailureEvent{} " + super.toString();
    }
}
