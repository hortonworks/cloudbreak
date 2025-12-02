package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaOrchestrationConfigService;

@Component
public class OrchestratorConfigHandler extends ExceptionCatcherEventHandler<OrchestratorConfigRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorConfigHandler.class);

    @Inject
    private FreeIpaOrchestrationConfigService freeIpaOrchestrationConfigService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(OrchestratorConfigRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<OrchestratorConfigRequest> event) {
        return new OrchestratorConfigFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<OrchestratorConfigRequest> event) {
        OrchestratorConfigRequest request = event.getData();
        Selectable response;
        try {
            freeIpaOrchestrationConfigService.configureOrchestrator(request.getResourceId());
            response = new OrchestratorConfigSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("FreeIPA orchestration configuration failed", e);
            response = new OrchestratorConfigFailed(request.getResourceId(), e, ERROR);
        }
        return response;
    }
}
