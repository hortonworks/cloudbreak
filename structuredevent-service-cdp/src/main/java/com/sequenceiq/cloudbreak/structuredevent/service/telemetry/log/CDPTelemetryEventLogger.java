package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public interface CDPTelemetryEventLogger<T extends CDPStructuredFlowEvent> {

    void log(T cdpStructuredFlowEvent);

    Class<T> acceptableEventClass();

}
