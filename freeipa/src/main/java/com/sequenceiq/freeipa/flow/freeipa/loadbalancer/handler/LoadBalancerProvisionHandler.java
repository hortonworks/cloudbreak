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
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerCreationService;

@Component
public class LoadBalancerProvisionHandler extends ExceptionCatcherEventHandler<LoadBalancerProvisionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerProvisionHandler.class);

    @Inject
    private FreeIpaLoadBalancerCreationService freeIpaLoadBalancerCreationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerProvisionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerProvisionRequest> event) {
        LOGGER.error("Unexpected error occurred while creating load balancer for FreeIPA", e);
        return new LoadBalancerCreationFailureEvent(resourceId, ERROR, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<LoadBalancerProvisionRequest> event) {
        Long stackId = event.getData().getResourceId();
        try {
            LOGGER.debug("Creating load balancer for FreeIPA cluster");
            freeIpaLoadBalancerCreationService.createLoadBalancer(event.getData());
            return new LoadBalancerProvisionSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Failed to provision FreeIPA load balancer", e);
            return new LoadBalancerCreationFailureEvent(stackId, ERROR, e);
        }
    }
}
