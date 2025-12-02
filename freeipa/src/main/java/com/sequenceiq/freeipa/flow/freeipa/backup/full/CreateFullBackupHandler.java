package com.sequenceiq.freeipa.flow.freeipa.backup.full;

import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.event.CreateFullBackupEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class CreateFullBackupHandler extends ExceptionCatcherEventHandler<CreateFullBackupEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateFullBackupHandler.class);

    @Inject
    private HostOrchestrator orchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaNodeUtilService nodeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateFullBackupEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateFullBackupEvent> event) {
        LOGGER.error("Unexpected error happened during backup creation", e);
        return new StackFailureEvent(FULL_BACKUP_FAILED_EVENT.event(), resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CreateFullBackupEvent> event) {
        Stack stack = stackService.getByIdWithListsInTransaction(event.getData().getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Set<Node> nodes = nodeService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());
        OrchestratorStateParams stateParameters = createOrchestratorStateParams(stack);
        try {
            runBackupForNodesSequentially(nodes, stateParameters);
            return new StackEvent(FullBackupEvent.FULL_BACKUP_SUCCESSFUL_EVENT.event(), event.getData().getResourceId());
        } catch (CloudbreakOrchestratorFailedException | CloneNotSupportedException e) {
            LOGGER.error("Full backup failed for node: {}", stateParameters.getTargetHostNames(), e);
            return new StackFailureEvent(FULL_BACKUP_FAILED_EVENT.event(), event.getData().getResourceId(), e, ERROR);
        }
    }

    private void runBackupForNodesSequentially(Set<Node> nodes, OrchestratorStateParams stateParameters)
            throws CloudbreakOrchestratorFailedException, CloneNotSupportedException {
        for (Node node : nodes) {
            OrchestratorStateParams orchestratorStateParams = stateParameters.clone();
            LOGGER.info("Run full backup for {}", node);
            orchestratorStateParams.setTargetHostNames(Set.of(node.getHostname()));
            orchestrator.runOrchestratorState(orchestratorStateParams);
        }
    }

    private OrchestratorStateParams createOrchestratorStateParams(Stack stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        OrchestratorStateParams stateParameters = new OrchestratorStateParams();
        stateParameters.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParameters.setState("freeipa.backup-full");
        LOGGER.debug("Created OrchestratorStateParams for running full backup: {}", stateParameters);
        return stateParameters;
    }
}
