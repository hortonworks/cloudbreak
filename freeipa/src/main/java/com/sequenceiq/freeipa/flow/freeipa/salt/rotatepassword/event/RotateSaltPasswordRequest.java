package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordType;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RotateSaltPasswordRequest extends StackEvent {

    private final RotateSaltPasswordReason reason;

    private final RotateSaltPasswordType type;

    @JsonCreator
    public RotateSaltPasswordRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("reason") RotateSaltPasswordReason reason,
            @JsonProperty("type") RotateSaltPasswordType type) {
        super(stackId);
        this.reason = reason;
        this.type = type;
    }

    public RotateSaltPasswordReason getReason() {
        return reason;
    }

    public RotateSaltPasswordType getType() {
        return type;
    }
}
