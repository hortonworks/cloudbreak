package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

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

    @Override
    public String toString() {
        return "RotateSaltPasswordRequest{" +
                super.toString() +
                ", reason=" + reason +
                ", type=" + type +
                '}';
    }
}
