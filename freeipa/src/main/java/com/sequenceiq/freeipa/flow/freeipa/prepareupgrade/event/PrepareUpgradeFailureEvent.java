package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailureEvent;

public class PrepareUpgradeFailureEvent extends FreeIpaFailureEvent {

    @JsonCreator
    public PrepareUpgradeFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception ex) {
        super(stackId, failureType, ex);
    }

    @Override
    public String toString() {
        return "PrepareUpgradeFailureEvent{" +
                super.toString() +
                '}';
    }
}
