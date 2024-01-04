package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsSuccess;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RegisterFreeIpaDnsHandler extends ExceptionCatcherEventHandler<RegisterFreeIpaDnsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterFreeIpaDnsHandler.class);

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterFreeIpaDnsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RegisterFreeIpaDnsRequest> event) {
        return new RegisterFreeIpaDnsFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RegisterFreeIpaDnsRequest> event) {
        RegisterFreeIpaDnsRequest request = event.getData();
        StackView stack = stackDtoService.getStackViewById(request.getResourceId());
        try {
            LOGGER.info("Registering load balancer DNS entry with FreeIPA");
            clusterPublicEndpointManagementService.registerLoadBalancerWithFreeIPA(stack);
            LOGGER.info("Load balancer FreeIPA DNS registration was successful");
            return new RegisterFreeIpaDnsSuccess(stack.getId());
        } catch (Exception e) {
            LOGGER.warn("Failed to register load balancers with FreeIPA.", e);
            return new RegisterFreeIpaDnsFailure(request.getResourceId(), e);
        }
    }
}
