package com.sequenceiq.freeipa.flow.stack.termination.handler;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.recipes.ExecutePreTerminationRecipesFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.recipes.ExecutePreTerminationRecipesRequest;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@Component
public class ExecutePreTerminationRecipesHandler extends ExceptionCatcherEventHandler<ExecutePreTerminationRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutePreTerminationRecipesHandler.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private StackService stackService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecutePreTerminationRecipesRequest> event) {
        return new StackFailureEvent(StackTerminationEvent.TERMINATION_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExecutePreTerminationRecipesRequest> event) {
        ExecutePreTerminationRecipesRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
            Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
            LOGGER.info("Executing pre-termination recipes");
            hostOrchestrator.preTerminationRecipes(primaryGatewayConfig, allNodes, StackBasedExitCriteriaModel.nonCancellableModel(), request.getForced());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Pre-termination recipe execution failed", e);
            return new StackFailureEvent(EventSelectorUtil.failureSelector(TerminateStackResult.class), stackId, e);
        } catch (CloudbreakOrchestratorTimeoutException e) {
            LOGGER.error("Pre-termination recipe execution timed out", e);
            return new StackFailureEvent(EventSelectorUtil.failureSelector(TerminateStackResult.class), stackId, e);
        }
        return new ExecutePreTerminationRecipesFinished(stackId, request.getForced());
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecutePreTerminationRecipesRequest.class);
    }

}
