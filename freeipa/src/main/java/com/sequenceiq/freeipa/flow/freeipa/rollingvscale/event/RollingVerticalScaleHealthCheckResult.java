package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RollingVerticalScaleHealthCheckResult extends StackEvent {

    private final boolean healthy;

    @JsonCreator
    public RollingVerticalScaleHealthCheckResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("healthy") boolean healthy) {
        super(stackId);
        this.healthy = healthy;
    }

    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public String toString() {
        return "RollingVerticalScaleHealthCheckResult{" +
                "healthy=" + healthy +
                "} " + super.toString();
    }
}
