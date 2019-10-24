package com.sequenceiq.freeipa.flow.stack.termination.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy.ClusterProxyDeregistrationFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy.ClusterProxyDeregistrationRequest;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyDeregistrationHandler implements EventHandler<ClusterProxyDeregistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyDeregistrationHandler.class);

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public void accept(Event<ClusterProxyDeregistrationRequest> requestEvent) {
        ClusterProxyDeregistrationRequest request = requestEvent.getData();
        LOGGER.debug("De-registering freeipa stack {} from cluster proxy", request.getResourceId());
        if (clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
            try {
                clusterProxyService.deregisterFreeIpa(request.getResourceId());
            } catch (Exception ex) {
                LOGGER.error("Cluster proxy de-registration failed", ex);
            }
        } else {
            LOGGER.debug("Cluster proxy integration not enabled. Skipping de-registration");
        }
        Selectable result = new ClusterProxyDeregistrationFinished(request.getResourceId(), request.getForced());
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyDeregistrationRequest.class);
    }
}
