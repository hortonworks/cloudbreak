package com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StopTelemetryResponse extends StackEvent {

    public StopTelemetryResponse(Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "StopTelemetryResponse{" +
                super.toString() +
                '}';
    }
}