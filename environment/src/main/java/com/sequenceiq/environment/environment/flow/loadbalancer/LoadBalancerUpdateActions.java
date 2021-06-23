package com.sequenceiq.environment.environment.flow.loadbalancer;

import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.FINALIZE_LOAD_BALANCER_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.HANDLED_FAILED_LOAD_BALANCER_UPDATE_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateFailedEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class LoadBalancerUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerUpdateActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public LoadBalancerUpdateActions(EnvironmentStatusUpdateService environmentStatusUpdateService, EnvironmentMetricService metricService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "ENVIRONMENT_UPDATE_STATE")
    public Action<?, ?> environmentLoadBalancerUpdate() {
        return new AbstractLoadBalancerUpdateAction<>(LoadBalancerUpdateEvent.class) {

            @Override
            protected void doExecute(CommonContext context, LoadBalancerUpdateEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.LOAD_BALANCER_ENV_UPDATE_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_LOAD_BALANCER_ENV_UPDATE_STARTED;
                LoadBalancerUpdateState loadBalancerUpdateState = LoadBalancerUpdateState.ENVIRONMENT_UPDATE_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                    loadBalancerUpdateState);
                EnvironmentLoadBalancerDto environmentLoadBalancerDto = EnvironmentLoadBalancerDto.builder()
                    .withId(envDto.getId())
                    .withEnvironmentDto(payload.getEnvironmentDto())
                    .withEndpointAccessGateway(payload.getEndpointAccessGateway())
                    .withEndpointGatewaySubnetIds(payload.getSubnetIds())
                    .build();
                sendEvent(context, LoadBalancerUpdateHandlerSelectors.ENVIRONMENT_UPDATE_HANDLER_EVENT.selector(), environmentLoadBalancerDto);
            }
        };
    }

    @Bean(name = "STACK_UPDATE_STATE")
    public Action<?, ?> stackLoadBalancerUpdate() {
        return new AbstractLoadBalancerUpdateAction<>(LoadBalancerUpdateEvent.class) {

            @Override
            protected void doExecute(CommonContext context, LoadBalancerUpdateEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.LOAD_BALANCER_STACK_UPDATE_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_LOAD_BALANCER_STACK_UPDATE_STARTED;
                LoadBalancerUpdateState loadBalancerUpdateState = LoadBalancerUpdateState.STACK_UPDATE_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                    loadBalancerUpdateState);
                EnvironmentLoadBalancerDto environmentLoadBalancerDto = EnvironmentLoadBalancerDto.builder()
                    .withId(envDto.getId())
                    .withEnvironmentDto(payload.getEnvironmentDto())
                    .withEndpointAccessGateway(payload.getEndpointAccessGateway())
                    .withEndpointGatewaySubnetIds(payload.getSubnetIds())
                    .withFlowId(context.getFlowId())
                    .build();
                sendEvent(context, LoadBalancerUpdateHandlerSelectors.STACK_UPDATE_HANDLER_EVENT.selector(), environmentLoadBalancerDto);
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_UPDATE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractLoadBalancerUpdateAction<>(ResourceCrnPayload.class) {
            @Override
            protected void doExecute(CommonContext context, ResourceCrnPayload payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                    .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                        ResourceEvent.ENVIRONMENT_LOAD_BALANCER_UPDATE_FINISHED, LoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FINISHED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_LOAD_BALANCER_UPDATE_FINISHED, environmentDto);
                LOGGER.info("Flow entered into LOAD_BALANCER_UPDATE_FINISHED_STATE");
                sendEvent(context, FINALIZE_LOAD_BALANCER_UPDATE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_UPDATE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractLoadBalancerUpdateAction<>(LoadBalancerUpdateFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, LoadBalancerUpdateFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn(String.format("Failed to update load balancer on environment '%s'. Status: '%s'.",
                    payload.getEnvironmentDto(), payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                    .updateFailedEnvironmentStatusAndNotify(context, payload, payload.getEnvironmentStatus(),
                        convertStatus(payload.getEnvironmentStatus()), LoadBalancerUpdateState.LOAD_BALANCER_UPDATE_FAILED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_LOAD_BALANCER_UPDATE_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_LOAD_BALANCER_UPDATE_EVENT.event(), payload);
            }
        };
    }

    private ResourceEvent convertStatus(EnvironmentStatus status) {
        switch (status) {
            case LOAD_BALANCER_ENV_UPDATE_FAILED:
                return ResourceEvent.ENVIRONMENT_LOAD_BALANCER_ENV_UPDATE_FAILED;
            case LOAD_BALANCER_STACK_UPDATE_FAILED:
                return ResourceEvent.ENVIRONMENT_LOAD_BALANCER_STACK_UPDATE_FAILED;
            default:
                return ResourceEvent.ENVIRONMENT_LOAD_BALANCER_UPDATE_FAILED;
        }
    }
}
