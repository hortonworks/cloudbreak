package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.BootstrapNewNodesEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class ClusterProxyReRegistrationHandler implements EventHandler<ClusterProxyReRegistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyReRegistrationHandler.class);

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

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
        if (!clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
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
