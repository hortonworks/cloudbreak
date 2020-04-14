package com.sequenceiq.freeipa.flow.stack.provision.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyRegistrationHandler implements EventHandler<ClusterProxyRegistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRegistrationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyRegistrationRequest.class);
    }

    @Override
    public void accept(Event<ClusterProxyRegistrationRequest> event) {
        ClusterProxyRegistrationRequest request = event.getData();
        Selectable response;
        try {
            clusterProxyService.registerBootstrapFreeIpa(request.getResourceId());
            response = new ClusterProxyRegistrationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Cluster Proxy bootstrap registration has failed", e);
            response = new ClusterProxyRegistrationFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
