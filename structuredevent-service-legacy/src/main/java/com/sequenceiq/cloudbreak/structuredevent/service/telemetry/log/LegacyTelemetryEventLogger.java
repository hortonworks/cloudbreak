package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

public interface LegacyTelemetryEventLogger {

    void log(StructuredFlowEvent structuredFlowEvent);

}
