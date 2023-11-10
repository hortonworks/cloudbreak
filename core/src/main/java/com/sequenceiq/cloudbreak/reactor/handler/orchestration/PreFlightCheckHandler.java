package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class PreFlightCheckHandler implements EventHandler<PreFlightCheckRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreFlightCheckHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PreFlightCheckRequest.class);
    }

    @Override
    public void accept(Event<PreFlightCheckRequest> event) {
        PreFlightCheckRequest data = event.getData();
        Long resourceId = data.getResourceId();
        try {
            diagnosticsFlowService.nodeStatusNetworkReport(resourceId);
            diagnosticsFlowService.nodeStatusMeteringReport(resourceId);
        } catch (Exception e) {
            LOGGER.debug("Error occurred during pre-flight node status checks (skipping): {}", e.getMessage());
        }
        PreFlightCheckSuccess result = new PreFlightCheckSuccess(resourceId);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
