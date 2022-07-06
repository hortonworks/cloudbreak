package com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StopTelemetryRequest extends StackEvent {

    private final List<String> instanceIds;

    @JsonCreator
    public StopTelemetryRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(stackId);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

}
