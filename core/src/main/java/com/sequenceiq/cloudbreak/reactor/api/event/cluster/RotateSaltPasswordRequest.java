package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

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
