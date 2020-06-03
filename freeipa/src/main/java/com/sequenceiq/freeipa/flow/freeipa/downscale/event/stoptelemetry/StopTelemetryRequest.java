package com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry;

import java.util.List;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StopTelemetryRequest extends StackEvent {

    private final List<String> instanceIds;

    public StopTelemetryRequest(Long stackId, List<String> instanceIds) {
        super(stackId);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

}
