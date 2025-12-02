package com.sequenceiq.freeipa.flow.stack;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;

public class HealthCheckFailed extends StackFailureEvent {

    private final List<String> instanceIds;

    @JsonCreator
    public HealthCheckFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failureType") FailureType failureType) {
        super(stackId, exception, failureType);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
