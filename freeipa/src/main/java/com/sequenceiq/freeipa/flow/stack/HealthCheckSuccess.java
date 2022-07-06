package com.sequenceiq.freeipa.flow.stack;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthCheckSuccess extends StackEvent {

    private final List<String> instanceIds;

    @JsonCreator
    public HealthCheckSuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(stackId);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
