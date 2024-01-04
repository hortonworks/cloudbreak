package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbSuccess;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RestartCmForLbHandler extends ExceptionCatcherEventHandler<RestartCmForLbRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartCmForLbHandler.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RestartCmForLbRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RestartCmForLbRequest> event) {
        return new RestartCmForLbFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RestartCmForLbRequest> event) {
        RestartCmForLbRequest request = event.getData();
        StackDto stackDto = stackDtoService.getById(event.getData().getResourceId());
        requireNonNull(stackDto.getStack());
        requireNonNull(stackDto.getCluster());
        try {
            LOGGER.debug("Restarting CM server to pick up latest config changes");
            restartCMServer(stackDto);
            clusterApiConnectors.getConnector(stackDto).waitForServer(false);
            LOGGER.debug("CM server restart was successful");
            return new RestartCmForLbSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.warn("Unable to restart CM server.", e);
            return new RestartCmForLbFailure(request.getResourceId(), e);
        }
    }

    private void restartCMServer(StackDto stackDto) throws Exception {
        ClusterView cluster = stackDto.getCluster();
        StackView stack = stackDto.getStack();
        InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId());
        hostOrchestrator.restartClusterManagerOnMaster(gatewayConfig, gatewayFQDN, exitModel);
    }
}
