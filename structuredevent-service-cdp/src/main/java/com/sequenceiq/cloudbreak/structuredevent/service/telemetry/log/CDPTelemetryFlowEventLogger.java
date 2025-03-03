package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public interface CDPTelemetryFlowEventLogger<T extends CDPStructuredFlowEvent> extends CDPTelemetryEventLogger<T> {

    Class<T> acceptableEventClass();

}
