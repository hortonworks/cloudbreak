package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.BootstrapNewNodesEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyReRegistrationHandler implements EventHandler<ClusterProxyReRegistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyReRegistrationHandler.class);

    @Inject
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyReRegistrationRequest.class);
    }

    @Override
    public void accept(Event<ClusterProxyReRegistrationRequest> event) {
        ClusterProxyReRegistrationRequest request = event.getData();
        Selectable response = registerCluster(request);
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }

    private Selectable registerCluster(ClusterProxyReRegistrationRequest request) {
        if (!clusterProxyEnablementService.isClusterProxyApplicable(request.getCloudPlatform())) {
            LOGGER.info("Cluster Proxy integration is DISABLED, skipping re-registering with Cluster Proxy service");
            return new BootstrapNewNodesEvent(StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT.event(), request.getResourceId());
        }

        Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
        try {
            clusterProxyService.reRegisterCluster(stack);
            return new BootstrapNewNodesEvent(StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT.event(), stack.getId());
        } catch (Exception e) {
            LOGGER.error("Error occurred re-registering cluster {} in environment {} to cluster proxy",
                    stack.getCluster().getId(), stack.getEnvironmentCrn(), e);
            return new ClusterProxyReRegistrationResult(e.getMessage(), e, request);
        }
    }

}
