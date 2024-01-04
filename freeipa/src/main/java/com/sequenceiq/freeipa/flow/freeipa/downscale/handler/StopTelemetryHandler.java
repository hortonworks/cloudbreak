package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryResponse;
import com.sequenceiq.freeipa.service.telemetry.TelemetryAgentService;

@Component
public class StopTelemetryHandler implements EventHandler<StopTelemetryRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopTelemetryHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private TelemetryAgentService telemetryAgentService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopTelemetryRequest.class);
    }

    @Override
    public void accept(Event<StopTelemetryRequest> event) {
        StopTelemetryRequest request = event.getData();
        LOGGER.info("Stop telemetry agents gracefully (if needed)...");
        telemetryAgentService.stopTelemetryAgent(request.getResourceId(), request.getInstanceIds());
        StopTelemetryResponse response = new StopTelemetryResponse(request.getResourceId());
        eventBus.notify(EventSelectorUtil.selector(StopTelemetryResponse.class),
                new Event<>(event.getHeaders(), response));
    }
}
