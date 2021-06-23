package com.sequenceiq.environment.environment.flow.loadbalancer.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateFailedEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateHandlerSelectors;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors;
import com.sequenceiq.environment.environment.service.LoadBalancerPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class LoadBalancerStackUpdateHandler extends EventSenderAwareHandler<EnvironmentLoadBalancerDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerStackUpdateHandler.class);

    private final LoadBalancerPollerService loadBalancerPollerService;

    protected LoadBalancerStackUpdateHandler(EventSender eventSender, LoadBalancerPollerService loadBalancerPollerService) {
        super(eventSender);
        this.loadBalancerPollerService = loadBalancerPollerService;
    }

    @Override
    public String selector() {
        return LoadBalancerUpdateHandlerSelectors.STACK_UPDATE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentLoadBalancerDto> envLoadBalancerDtoEvent) {
        EnvironmentLoadBalancerDto environmentLoadBalancerDto = envLoadBalancerDtoEvent.getData();
        EnvironmentDto environmentDto = environmentLoadBalancerDto.getEnvironmentDto();
        try {
            LOGGER.debug("Initating stack load balancer update");
            loadBalancerPollerService.updateStackWithLoadBalancer(environmentDto.getId(), environmentDto.getResourceCrn(),
                environmentDto.getName(), environmentLoadBalancerDto.getEndpointAccessGateway(), environmentLoadBalancerDto.getFlowId());

            LOGGER.debug("Stack load balancer update complete.");
            LoadBalancerUpdateEvent loadBalancerUpdateEvent = LoadBalancerUpdateEvent.LoadBalancerUpdateEventBuilder.aLoadBalancerUpdateEvent()
                .withSelector(LoadBalancerUpdateStateSelectors.FINISH_LOAD_BALANCER_UPDATE_EVENT.selector())
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withEnvironmentDto(environmentDto)
                .withPublicEndpointAccessGateway(environmentLoadBalancerDto.getEndpointAccessGateway())
                .withSubnetIds(environmentLoadBalancerDto.getEndpointGatewaySubnetIds())
                .build();
            eventSender().sendEvent(loadBalancerUpdateEvent, envLoadBalancerDtoEvent.getHeaders());
        } catch (Exception e) {
            LoadBalancerUpdateFailedEvent failedEvent = new LoadBalancerUpdateFailedEvent(environmentDto, e,
                EnvironmentStatus.LOAD_BALANCER_STACK_UPDATE_FAILED);
            eventSender().sendEvent(failedEvent, envLoadBalancerDtoEvent.getHeaders());
        }
    }
}
