package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.CLUSTER_PROXY_DEREGISTER_FAILED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
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

    @Inject
    private ClusterProxyEnablementService clusterProxyEnablementService;

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
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            if (clusterProxyEnablementService.isClusterProxyApplicable(request.getCloudPlatform())) {
                try {
                    clusterProxyService.deregisterCluster(stack);
                } catch (Exception ex) {
                    LOGGER.warn("Cluster proxy deregister failed", ex);
                }
            } else {
                LOGGER.info("Cluster Proxy integration is DISABLED, skipping de-registering with Cluster Proxy service. Cluster CRN: {}",
                        stack.getResourceCrn());
            }
            stackTerminationService.deleteDnsEntry(stack);
            result = new ClusterProxyDeregisterSuccess(stack.getId());
        } catch (Exception ex) {
            LOGGER.warn("Cluster proxy deregister failed", ex);
            result = new StackFailureEvent(CLUSTER_PROXY_DEREGISTER_FAILED_EVENT.event(), request.getResourceId(), ex);
        }
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyDeregisterRequest.class);
    }
}
