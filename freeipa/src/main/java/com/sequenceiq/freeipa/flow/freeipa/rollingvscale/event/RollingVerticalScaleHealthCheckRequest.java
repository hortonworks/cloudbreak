package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RollingVerticalScaleHealthCheckRequest extends StackEvent {

    private final String instanceId;

    @JsonCreator
    public RollingVerticalScaleHealthCheckRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceId") String instanceId) {
        super(stackId);
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String toString() {
        return "RollingVerticalScaleHealthCheckRequest{" +
                "instanceId='" + instanceId + '\'' +
                "} " + super.toString();
    }
}
