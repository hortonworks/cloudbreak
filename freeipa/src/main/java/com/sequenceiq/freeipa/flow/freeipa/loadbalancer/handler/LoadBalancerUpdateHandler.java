package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerUpdateService;

@Component
public class LoadBalancerUpdateHandler extends ExceptionCatcherEventHandler<LoadBalancerUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerUpdateHandler.class);

    @Inject
    private FreeIpaLoadBalancerUpdateService loadBalancerUpdateService;

    @Inject
    private FreeIpaLoadBalancerService loadBalancerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerUpdateRequest> event) {
        LOGGER.error("Unexpected error occurred while updating load balancer for FreeIPA", e);
        return new LoadBalancerUpdateFailureEvent(resourceId, ERROR, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<LoadBalancerUpdateRequest> event) {
        Long stackId = event.getData().getResourceId();
        Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(stackId);
        if (loadBalancer.isPresent()) {
            try {
                LOGGER.debug("Updating load balancer for FreeIPA cluster");
                loadBalancerUpdateService.updateLoadBalancer(event.getData());
                return new LoadBalancerUpdateSuccess(stackId);
            } catch (Exception e) {
                LOGGER.error("Failed to update FreeIPA load balancer", e);
                return new LoadBalancerUpdateFailureEvent(stackId, ERROR, e);
            }
        } else {
            LOGGER.debug("No load balancer found for FreeIPA cluster");
            return new LoadBalancerUpdateSuccess(stackId);
        }
    }
}
