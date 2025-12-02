package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreSuccess;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaRestoreHandler extends ExceptionCatcherEventHandler<FreeIpaRestoreRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRestoreHandler.class);

    private static final String FREEIPA_REBUILD_RESTORE_STATE = "freeipa/rebuild/restore";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaRestoreRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaRestoreRequest> event) {
        return new FreeIpaRestoreFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaRestoreRequest> event) {
        FreeIpaRestoreRequest request = event.getData();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            OrchestratorStateParams stateParams = new OrchestratorStateParams();
            stateParams.setTargetHostNames(Set.of(primaryGatewayConfig.getHostname()));
            stateParams.setPrimaryGatewayConfig(primaryGatewayConfig);
            stateParams.setExitCriteriaModel(new StackBasedExitCriteriaModel(stack.getId()));
            stateParams.setState(FREEIPA_REBUILD_RESTORE_STATE);
            hostOrchestrator.runOrchestratorState(stateParams);
            return new FreeIpaRestoreSuccess(request.getResourceId());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("FreeIpa restore failed", e);
            return new FreeIpaRestoreFailed(request.getResourceId(), e, ERROR);
        }
    }
}
