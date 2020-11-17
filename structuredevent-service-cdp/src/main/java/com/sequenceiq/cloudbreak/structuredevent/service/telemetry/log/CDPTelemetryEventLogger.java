package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public interface CDPTelemetryEventLogger {

    void log(CDPStructuredFlowEvent cdpStructuredFlowEvent);

}
