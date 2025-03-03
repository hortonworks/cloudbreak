package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log;

public interface CDPTelemetryEventLogger<T> {

    void log(T cdpStructuredEvent);

}
