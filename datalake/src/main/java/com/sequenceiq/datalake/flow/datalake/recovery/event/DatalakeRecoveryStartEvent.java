package com.sequenceiq.datalake.flow.datalake.recovery.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

public class DatalakeRecoveryStartEvent extends SdxEvent {

    // TODO: RecoveryType will be used in the context of
    //  automatic data restore included in the flow
    private final SdxRecoveryType recoveryType;

    @JsonCreator
    public DatalakeRecoveryStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("recoveryType") SdxRecoveryType recoveryType) {
        super(selector, sdxId, userId);
        this.recoveryType = recoveryType;
    }

    public SdxRecoveryType getRecoveryType() {
        return recoveryType;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeRecoveryStartEvent.class, other,
                event -> Objects.equals(recoveryType, event.recoveryType));
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryStartEvent{" +
                "recoveryType=" + recoveryType +
                "} " + super.toString();
    }
}
