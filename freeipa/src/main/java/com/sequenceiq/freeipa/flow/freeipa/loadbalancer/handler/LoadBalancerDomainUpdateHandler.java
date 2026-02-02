package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;

@Component
public class LoadBalancerDomainUpdateHandler extends ExceptionCatcherEventHandler<LoadBalancerDomainUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerDomainUpdateHandler.class);

    @Inject
    private FreeIpaLoadBalancerDomainService loadBalancerDomainService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerDomainUpdateRequest> event) {
        return new LoadBalancerCreationFailureEvent(resourceId, FailureType.ERROR, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<LoadBalancerDomainUpdateRequest> event) {
        Long stackId = event.getData().getResourceId();
        try {
            loadBalancerDomainService.registerLbDomain(stackId);
            return new LoadBalancerDomainUpdateSuccess(stackId);
        } catch (FreeIpaClientException | PemDnsEntryCreateOrUpdateException e) {
            LOGGER.error("Failed to update FreeIPA load balancer domain", e);
            return new LoadBalancerCreationFailureEvent(stackId, FailureType.ERROR, e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerDomainUpdateRequest.class);
    }
}
