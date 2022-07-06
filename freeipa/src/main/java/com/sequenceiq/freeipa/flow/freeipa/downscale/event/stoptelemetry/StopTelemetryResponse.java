package com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StopTelemetryResponse extends StackEvent {

    @JsonCreator
    public StopTelemetryResponse(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "StopTelemetryResponse{" +
                super.toString() +
                '}';
    }
}
