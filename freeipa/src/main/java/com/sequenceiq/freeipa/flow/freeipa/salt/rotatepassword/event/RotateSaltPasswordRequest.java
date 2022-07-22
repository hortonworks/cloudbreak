package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RotateSaltPasswordRequest extends StackEvent {

    private final RotateSaltPasswordReason reason;

    @JsonCreator
    public RotateSaltPasswordRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("reason") RotateSaltPasswordReason reason) {
        super(stackId);
        this.reason = reason;
    }

    public RotateSaltPasswordReason getReason() {
        return reason;
    }
}
