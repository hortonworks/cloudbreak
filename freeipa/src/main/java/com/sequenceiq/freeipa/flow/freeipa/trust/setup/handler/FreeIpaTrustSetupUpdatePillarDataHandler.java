package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataSuccess;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.freeipa.flow.SaltConfigProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaTrustSetupUpdatePillarDataHandler extends ExceptionCatcherEventHandler<FreeIpaTrustSetupUpdatePillarDataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupUpdatePillarDataHandler.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private SaltConfigProvider saltConfigProvider;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustSetupUpdatePillarDataRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustSetupUpdatePillarDataRequest> event) {
        return new FreeIpaTrustSetupUpdatePillarDataFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustSetupUpdatePillarDataRequest> event) {
        FreeIpaTrustSetupUpdatePillarDataRequest request = event.getData();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
            Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
            SaltConfig saltConfig = saltConfigProvider.getSaltConfig(stack, allNodes);
            OrchestratorStateParams stateParams = new OrchestratorStateParams();
            stateParams.setTargetHostNames(allNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            stateParams.setPrimaryGatewayConfig(gatewayConfig);
            hostOrchestrator.saveCustomPillars(saltConfig, null, stateParams);
            return new FreeIpaTrustSetupUpdatePillarDataSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to update pillar data", e);
            return new FreeIpaTrustSetupUpdatePillarDataFailed(request.getResourceId(), e);
        }
    }
}
