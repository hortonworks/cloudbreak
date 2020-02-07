package com.sequenceiq.freeipa.flow.stack.termination.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.stack.termination.event.telemetry.StopTelemetryAgentFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.telemetry.StopTelemetryAgentRequest;
import com.sequenceiq.freeipa.service.TelemetryAgentService;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopTelemetryAgentHandler implements EventHandler<StopTelemetryAgentRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopTelemetryAgentHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private TelemetryAgentService telemetryAgentService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopTelemetryAgentRequest.class);
    }

    @Override
    public void accept(Event<StopTelemetryAgentRequest> event) {
        StopTelemetryAgentRequest request = event.getData();
        LOGGER.info("Stop telemetry agents gracefully (if needed)...");
        telemetryAgentService.stopTelemetryAgent(request.getResourceId());
        StopTelemetryAgentFinished response = new StopTelemetryAgentFinished(request.getResourceId(), request.getForced());
        eventBus.notify(EventSelectorUtil.selector(StopTelemetryAgentFinished.class),
                new Event<>(event.getHeaders(), response));
    }
}
