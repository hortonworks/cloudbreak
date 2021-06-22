package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class PreFlightCheckHandler implements EventHandler<PreFlightCheckRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreFlightCheckHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PreFlightCheckRequest.class);
    }

    @Override
    public void accept(Event<PreFlightCheckRequest> event) {
        PreFlightCheckRequest data = event.getData();
        Long resourceId = data.getResourceId();
        try {
            diagnosticsFlowService.collectNodeStatusTelemetry(resourceId);
        } catch (Exception e) {
            LOGGER.debug("Error occurred during pre-flight node status telemetry collection (skipping): {}", e.getMessage());
        }
        try {
            Stack stack = stackService.getById(resourceId);
            boolean useNotification = entitlementService.networkPreflightNotificationsEnabled(
                    Crn.safeFromString(stack.getResourceCrn()).getAccountId());
            diagnosticsFlowService.nodeStatusNetworkReport(resourceId, useNotification);
        } catch (Exception e) {
            LOGGER.debug("Error occurred during pre-flight network status checks (skipping): {}", e.getMessage());
        }
        PreFlightCheckSuccess result = new PreFlightCheckSuccess(resourceId);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
