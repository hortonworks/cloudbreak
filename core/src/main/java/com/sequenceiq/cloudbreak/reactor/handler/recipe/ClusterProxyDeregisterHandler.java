package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyDeregisterHandler implements EventHandler<ClusterProxyDeregisterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyDeregisterHandler.class);

    @Value("${clusterProxy.enabled:false}")
    private boolean clusterProxyIntegrationEnabled;

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private StackTerminationService stackTerminationService;

    @Override
    public void accept(Event<ClusterProxyDeregisterRequest> requestEvent) {
        ClusterProxyDeregisterRequest request = requestEvent.getData();
        Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
        if (clusterProxyIntegrationEnabled) {
            try {
                clusterProxyService.deregisterCluster(stack);
            } catch (Exception ex) {
                LOGGER.error("Cluster proxy deregister failed", ex);
            }
        }
        stackTerminationService.deleteDnsEntry(stack, null);
        Selectable result = new ClusterProxyDeregisterSuccess(stack.getId());
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyDeregisterRequest.class);
    }
}
