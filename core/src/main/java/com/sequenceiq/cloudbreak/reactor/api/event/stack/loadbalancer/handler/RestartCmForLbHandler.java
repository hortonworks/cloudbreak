package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbSuccess;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.util.StackUtil;
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
    private StackUtil stackUtil;

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
        Stack stack = request.getStack();
        requireNonNull(stack);
        try {
            LOGGER.debug("Restarting CM server to pick up latest config changes");
            restartCMServer(stack);
            clusterApiConnectors.getConnector(stack).waitForServer(stack, false);
            LOGGER.debug("CM server restart was successful");
            return new RestartCmForLbSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.warn("Unable to restart CM server.", e);
            return new RestartCmForLbFailure(request.getResourceId(), e);
        }
    }

    private void restartCMServer(Stack stack) throws Exception {
        Cluster cluster = stack.getCluster();
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance, cluster.hasGateway());
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId());
        hostOrchestrator.restartClusterManagerOnMaster(gatewayConfig, gatewayFQDN, stackUtil.collectReachableNodes(stack), exitModel);
    }
}
