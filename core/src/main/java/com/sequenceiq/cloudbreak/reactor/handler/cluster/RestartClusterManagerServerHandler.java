package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerSuccess;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RestartClusterManagerServerHandler  extends ExceptionCatcherEventHandler<RestartClusterManagerServerRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestartClusterManagerServerHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackUtil stackUtil;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RestartClusterManagerServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RestartClusterManagerServerRequest> event) {
        return new StackFailureEvent(event.getData().getFailureSelector(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RestartClusterManagerServerRequest> event) {
        LOGGER.debug("Accepting Cluster Manager restart request...");
        RestartClusterManagerServerRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable response;
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            restartCMServer(stackDto);
            clusterApiConnectors.getConnector(stackDto).waitForServer(request.isDefaultClusterManagerAuth());
            response = new RestartClusterManagerServerSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Cluster Manager restart failed", e);
            response = new StackFailureEvent(request.getFailureSelector(), request.getResourceId(), e);
        }
        return response;
    }

    private void restartCMServer(StackDto stackDto) throws Exception {
        ClusterView cluster = stackDto.getCluster();
        StackView stack = stackDto.getStack();
        InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId());
        Set<Node> nodes = stackUtil.collectReachableNodes(stackDto);
        Set<String> agentTargets = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        hostOrchestrator.updateAgentCertDirectoryPermission(gatewayConfig, agentTargets, exitModel);
        hostOrchestrator.restartClusterManagerOnMaster(gatewayConfig, gatewayFQDN, exitModel);
    }
}
