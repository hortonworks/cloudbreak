package com.sequenceiq.datalake.flow.loadbalancer.dns;

import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_SUCCESS_EVENT;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.StartUpdateLoadBalancerDNSEvent;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.UpdateLoadBalancerDNSFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.loadbalancer.dns.UpdateLoadBalancerDNSService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class UpdateLoadBalancerDNSActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateLoadBalancerDNSActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private UpdateLoadBalancerDNSService updateLoadBalancerDNSService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private EventSenderService eventSenderService;

    @Bean(name = "UPDATE_LOAD_BALANCER_DNS_STATE")
    public Action<?, ?> updateLoadBalancerDNSAction() {
        return new AbstractSdxAction<>(StartUpdateLoadBalancerDNSEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    StartUpdateLoadBalancerDNSEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StartUpdateLoadBalancerDNSEvent payload,
                    Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                LOGGER.info("Attempting to update the load balancer DNS for cluster {}.", sdxCluster.getClusterName());
                updateLoadBalancerDNSService.performLoadBalancerDNSUpdate(sdxCluster);
                LOGGER.info("Successfully updated the load balancer DNS for cluster {}.", sdxCluster.getClusterName());
                eventSenderService.notifyEvent(sdxCluster, context, ResourceEvent.UPDATE_LOAD_BALANCER_DNS_FINISHED);
                sendEvent(context, UPDATE_LOAD_BALANCER_DNS_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StartUpdateLoadBalancerDNSEvent payload, Optional<SdxContext> flowContext, Exception e) {
                LOGGER.error("Failed to update load balancer DNS of SDX with ID {}.", payload.getResourceId());
                return UpdateLoadBalancerDNSFailedEvent.from(payload, e);
            }
        };
    }

    @Bean(name = "UPDATE_LOAD_BALANCER_DNS_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(UpdateLoadBalancerDNSFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    UpdateLoadBalancerDNSFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, UpdateLoadBalancerDNSFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Update of load balancer DNS for SDX cluster with ID {} and name {} failed with error {}.",
                        payload.getResourceId(), payload.getSdxName(), exception.getMessage(), exception);
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                eventSenderService.sendEventAndNotification(
                        sdxCluster, context.getFlowTriggerUserCrn(), ResourceEvent.UPDATE_LOAD_BALANCER_DNS_FAILED,
                        Set.of(exception.getMessage())
                );
                getFlow(context.getFlowParameters().getFlowId()).setFlowFailed(payload.getException());
                sendEvent(context, UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(UpdateLoadBalancerDNSFailedEvent payload, Optional<SdxContext> flowContext, Exception e) {
                LOGGER.error("Critical error in update of load balancer DNS. Failure was not handled correctly.", e);
                return null;
            }
        };
    }
}
