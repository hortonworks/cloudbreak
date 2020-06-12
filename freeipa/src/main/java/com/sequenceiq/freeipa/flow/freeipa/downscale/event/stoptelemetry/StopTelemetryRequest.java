package com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry;

import java.util.List;

import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;

public class StopTelemetryRequest extends DownscaleEvent {

    public StopTelemetryRequest(Long stackId, List<String> instanceIds) {
        super(stackId, instanceIds);
    }

}
