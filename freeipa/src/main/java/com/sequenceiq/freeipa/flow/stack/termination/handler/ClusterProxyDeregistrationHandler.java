package com.sequenceiq.freeipa.flow.stack.termination.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy.ClusterProxyDeregistrationFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy.ClusterProxyDeregistrationRequest;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

@Component
public class ClusterProxyDeregistrationHandler implements EventHandler<ClusterProxyDeregistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyDeregistrationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private FreeIpaLoadBalancerDomainService freeIpaLoadBalancerDomainService;

    @Override
    public void accept(Event<ClusterProxyDeregistrationRequest> requestEvent) {
        ClusterProxyDeregistrationRequest request = requestEvent.getData();
        LOGGER.debug("De-registering freeipa stack {} from cluster proxy", request.getResourceId());
        try {
            clusterProxyService.deregisterFreeIpa(request.getResourceId());
            freeIpaLoadBalancerDomainService.deregisterLbDomain(request.getResourceId());
        } catch (Exception ex) {
            LOGGER.error("Cluster proxy de-registration failed", ex);
        }
        Selectable result = new ClusterProxyDeregistrationFinished(request.getResourceId(), request.getForced());
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyDeregistrationRequest.class);
    }
}
