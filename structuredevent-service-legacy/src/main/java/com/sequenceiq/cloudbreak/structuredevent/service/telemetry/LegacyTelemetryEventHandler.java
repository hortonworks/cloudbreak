package com.sequenceiq.cloudbreak.structuredevent.service.telemetry;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.LegacyTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.cluster.ClusterSyncLogger;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class LegacyTelemetryEventHandler<T extends StructuredEvent> implements EventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyTelemetryEventHandler.class);

    @Inject
    private List<LegacyTelemetryEventLogger> legacyTelemetryEventLoggers;

    @Inject
    private ClusterSyncLogger clusterSyncLogger;

    @Override
    public String selector() {
        return LegacyTelemetryAsyncEventSender.LEGACY_TELEMETRY_EVENT_LOG_MESSAGE;
    }

    @Override
    public void accept(Event<T> structuredEvent) {
        try {
            T data = structuredEvent.getData();

            if (data instanceof StructuredFlowEvent) {
                StructuredFlowEvent flowEvent = (StructuredFlowEvent) data;
                for (LegacyTelemetryEventLogger legacyTelemetryEventLogger : legacyTelemetryEventLoggers) {
                    legacyTelemetryEventLogger.log(flowEvent);
                }
            } else if (data instanceof StructuredSyncEvent) {
                StructuredSyncEvent syncEvent = (StructuredSyncEvent) data;
                clusterSyncLogger.log(syncEvent);
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot perform sending telemetry log!", e);
        }
    }
}