package com.sequenceiq.cloudbreak.structuredevent.service.telemetry;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.CDPTelemetryFlowEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.freeipa.CDPFreeIpaSyncLogger;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class TelemetryEventHandler<T extends CDPStructuredEvent> implements EventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryEventHandler.class);

    @Inject
    private List<CDPTelemetryFlowEventLogger> cdpTelemetryFlowEventLoggers;

    @Inject
    private CDPFreeIpaSyncLogger cdpFreeIpaSyncLogger;

    @Override
    public String selector() {
        return TelemetryAsyncEventSender.TELEMETRY_EVENT_LOG_MESSAGE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        try {
            T data = structuredEvent.getData();
            switch (data) {
                case null -> LOGGER.warn("Received null data in telemetry event handler! Event: {}", structuredEvent);
                case CDPStructuredFlowEvent cdpStructuredFlowEvent -> logFlowEvent(cdpStructuredFlowEvent);
                case CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent -> cdpFreeIpaSyncLogger.log(cdpFreeipaStructuredSyncEvent);
                default -> LOGGER.debug("We are not sending telemetry log for {}", data.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot perform sending telemetry log!", e);
        }
    }

    private void logFlowEvent(CDPStructuredFlowEvent cdpStructuredFlowEvent) {
        for (CDPTelemetryFlowEventLogger cdpTelemetryFlowEventLogger : cdpTelemetryFlowEventLoggers) {
            if (cdpTelemetryFlowEventLogger.acceptableEventClass().equals(cdpStructuredFlowEvent.getClass())) {
                cdpTelemetryFlowEventLogger.log(cdpStructuredFlowEvent);
            }
        }
    }
}
