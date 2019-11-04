package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationSuccess;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyRegistrationHandler implements EventHandler<ClusterProxyRegistrationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRegistrationHandler.class);

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private StackService stackService;

    @Inject
    private GatewayService gatewayService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyRegistrationRequest.class);
    }

    @Override
    public void accept(Event<ClusterProxyRegistrationRequest> event) {
        ClusterProxyRegistrationRequest request = event.getData();
        Selectable response = registerCluster(request);
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }

    private Selectable registerCluster(ClusterProxyRegistrationRequest request) {
        Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
        try {
            if (!clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
                LOGGER.warn("Cluster Proxy integration is DISABLED, skipping registering with Cluster Proxy service. Cluster CRN: {}", stack.getResourceCrn());
                return new ClusterProxyRegistrationSuccess(request.getResourceId());
            }
            ConfigRegistrationResponse registerResponse = clusterProxyService.registerCluster(stack, request.getAccountId());
            Cluster cluster = stack.getCluster();
            if (cluster.hasGateway()) {
                LOGGER.debug("Updating Gateway for cluster {} in environment {} with public key certificate retrieved from Cluster Proxy",
                        cluster.getId(), stack.getEnvironmentCrn());
                Gateway gateway = cluster.getGateway();
                gateway.setTokenCert(registerResponse.getX509Unwrapped());
                gatewayService.save(gateway);
            }
            return new ClusterProxyRegistrationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Error occurred when registering cluster {} in environment {} to cluster proxy",
                    stack.getCluster().getId(), stack.getEnvironmentCrn(), e);
            return new ClusterProxyRegistrationFailed(request.getResourceId(), e);
        }
    }
}
