package com.sequenceiq.freeipa.flow.stack;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthCheckFailed extends StackFailureEvent {

    private final List<String> instanceIds;

    @JsonCreator
    public HealthCheckFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
