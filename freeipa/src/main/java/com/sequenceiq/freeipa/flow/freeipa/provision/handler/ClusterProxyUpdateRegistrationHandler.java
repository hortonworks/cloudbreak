package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

@Component
public class ClusterProxyUpdateRegistrationHandler implements EventHandler<ClusterProxyUpdateRegistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyUpdateRegistrationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyUpdateRegistrationRequest.class);
    }

    @Override
    public void accept(Event<ClusterProxyUpdateRegistrationRequest> event) {
        ClusterProxyUpdateRegistrationRequest request = event.getData();
        Selectable response;
        try {
            clusterProxyService.updateFreeIpaRegistrationAndWait(request.getResourceId(), request.getInstanceIds());
            response = new ClusterProxyUpdateRegistrationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Updating Cluster Proxy registration has failed", e);
            response = new ClusterProxyUpdateRegistrationFailed(request.getResourceId(), e, ERROR);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
