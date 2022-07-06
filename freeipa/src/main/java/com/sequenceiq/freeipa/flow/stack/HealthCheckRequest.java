package com.sequenceiq.freeipa.flow.stack;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthCheckRequest extends StackEvent {

    private final boolean waitForFreeIpaAvailability;

    private final List<String> instanceIds;

    public HealthCheckRequest(Long stackId, boolean waitForFreeIpaAvailability) {
        this(stackId, waitForFreeIpaAvailability, null);
    }

    @JsonCreator
    public HealthCheckRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("waitForFreeIpaAvailability") boolean waitForFreeIpaAvailability,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(stackId);
        this.waitForFreeIpaAvailability = waitForFreeIpaAvailability;
        this.instanceIds = instanceIds;
    }

    public boolean isWaitForFreeIpaAvailability() {
        return waitForFreeIpaAvailability;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
