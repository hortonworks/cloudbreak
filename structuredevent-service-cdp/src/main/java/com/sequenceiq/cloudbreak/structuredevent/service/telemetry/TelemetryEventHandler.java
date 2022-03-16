package com.sequenceiq.cloudbreak.structuredevent.service.telemetry;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeIpaStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryEventLogger;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class TelemetryEventHandler<T extends CDPStructuredEvent> implements EventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryEventHandler.class);

    @Inject
    private List<CDPTelemetryEventLogger> cdpTelemetryEventLoggers;

    @Override
    public String selector() {
        return TelemetryAsyncEventSender.TELEMETRY_EVENT_LOG_MESSAGE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        try {
            T data = structuredEvent.getData();

            if (data instanceof CDPEnvironmentStructuredFlowEvent) {
                CDPEnvironmentStructuredFlowEvent flowEvent = (CDPEnvironmentStructuredFlowEvent) data;
                for (CDPTelemetryEventLogger cdpTelemetryEventLogger : cdpTelemetryEventLoggers) {
                    cdpTelemetryEventLogger.log(flowEvent);
                }
            } else if (data instanceof CDPFreeIpaStructuredFlowEvent) {
                CDPFreeIpaStructuredFlowEvent flowEvent = (CDPFreeIpaStructuredFlowEvent) data;
                for (CDPTelemetryEventLogger cdpTelemetryEventLogger : cdpTelemetryEventLoggers) {
                    cdpTelemetryEventLogger.log(flowEvent);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot perform sending telemetry log!", e);
        }
    }
}