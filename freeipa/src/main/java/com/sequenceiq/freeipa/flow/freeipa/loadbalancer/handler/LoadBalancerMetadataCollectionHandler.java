package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerMetadataCollectionService;

@Component
public class LoadBalancerMetadataCollectionHandler extends ExceptionCatcherEventHandler<LoadBalancerMetadataCollectionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerMetadataCollectionHandler.class);

    @Inject
    private FreeIpaLoadBalancerMetadataCollectionService freeIpaLoadBalancerMetadataCollectionService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerMetadataCollectionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerMetadataCollectionRequest> event) {
        LOGGER.error("Unexpected error occurred while collecting load balancer metadata", e);
        return new LoadBalancerCreationFailureEvent(resourceId, ERROR, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<LoadBalancerMetadataCollectionRequest> event) {
        Long stackId = event.getData().getResourceId();
        try {
            LOGGER.debug("Collection load balancer metadata from cloud provider");
            freeIpaLoadBalancerMetadataCollectionService.collectLoadBalancerMetadata(event.getData());
            return new LoadBalancerMetadataCollectionSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Failed to collect load balancer metadata", e);
            return new LoadBalancerCreationFailureEvent(stackId, ERROR, e);
        }
    }
}
