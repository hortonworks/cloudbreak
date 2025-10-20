package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.handler;

import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.SwitchFreeIpaMasterToPrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class SwitchFreeIpaMasterToPrimaryGatewayHandler extends ExceptionCatcherEventHandler<SwitchFreeIpaMasterToPrimaryGatewayEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchFreeIpaMasterToPrimaryGatewayHandler.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaNodeUtilService nodeService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SwitchFreeIpaMasterToPrimaryGatewayEvent> event) {
        return new ChangePrimaryGatewayFailureEvent(resourceId, "Switching FreeIPA master to Primary Gateway", Set.of(), Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SwitchFreeIpaMasterToPrimaryGatewayEvent> event) {
        Long stackId = event.getEvent().getData().getResourceId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<Node> nodes = nodeService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());
        try {
            hostOrchestrator.switchFreeIpaMasterToPrimaryGateway(primaryGatewayConfig, nodes, new StackBasedExitCriteriaModel(stackId));
            return new StackEvent(CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT.event(), stackId);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Switching FreeIPA master to Primary Gateway failed with exception. Failure ignored, moving to the next step.", e);
            return new StackEvent(CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT.event(), stackId);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SwitchFreeIpaMasterToPrimaryGatewayEvent.class);
    }
}
