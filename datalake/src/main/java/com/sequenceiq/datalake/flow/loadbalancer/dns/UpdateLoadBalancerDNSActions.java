package com.sequenceiq.datalake.flow.loadbalancer.dns;

import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_IPA_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_PEM_SUCCESS_EVENT;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

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
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeRecoveryFlowEventChainFactory;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.StartUpdateLoadBalancerDNSEvent;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.UpdateLoadBalancerDNSFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.loadbalancer.dns.UpdateLoadBalancerDNSService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Configuration
public class UpdateLoadBalancerDNSActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateLoadBalancerDNSActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private UpdateLoadBalancerDNSService updateLoadBalancerDNSService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Bean(name = "UPDATE_LOAD_BALANCER_DNS_PEM_STATE")
    public Action<?, ?> updateLoadBalancerDNSPEMAction() {
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
                LOGGER.info("Attempting to update the load balancer DNS on PEM for cluster {}.", sdxCluster.getClusterName());
                updateLoadBalancerDNSService.performLoadBalancerDNSUpdateOnPEM(sdxCluster);
                LOGGER.info("Successfully updated the load balancer DNS on PEM for cluster {}.", sdxCluster.getClusterName());
                eventSenderService.notifyEvent(sdxCluster, ResourceEvent.UPDATE_LOAD_BALANCER_DNS_PEM_FINISHED);
                sendEvent(context, UPDATE_LOAD_BALANCER_DNS_PEM_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StartUpdateLoadBalancerDNSEvent payload, Optional<SdxContext> flowContext, Exception e) {
                LOGGER.error("Failed to update load balancer DNS of SDX with ID {} on PEM.", payload.getResourceId());
                return UpdateLoadBalancerDNSFailedEvent.from(payload, e);
            }
        };
    }

    @Bean(name = "UPDATE_LOAD_BALANCER_DNS_IPA_STATE")
    public Action<?, ?> updateLoadBalancerDNSIPAAction() {
        return new AbstractSdxAction<>(StartUpdateLoadBalancerDNSEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    StartUpdateLoadBalancerDNSEvent payload) {
                SdxContext context = SdxContext.from(flowParameters, payload);

                // When SDX is created as part of re-size flow chain, SDX in payload will not have the correct ID.
                if (flowChainLogService.isFlowTriggeredByFlowChain(
                        DatalakeResizeFlowEventChainFactory.class.getSimpleName(),
                        flowLogService.getLastFlowLog(flowParameters.getFlowId()))) {
                    SdxCluster sdxCluster = sdxService.getByNameInAccount(payload.getUserId(), payload.getSdxName());
                    LOGGER.info("Updating the Sdx-id in context from {} to {}", payload.getResourceId(), sdxCluster.getId());
                    context.setSdxId(sdxCluster.getId());
                }
                return context;
            }

            @Override
            protected void doExecute(SdxContext context, StartUpdateLoadBalancerDNSEvent payload,
                    Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxService.getById(context.getSdxId());
                LOGGER.info("Attempting to update the load balancer DNS on Free IPA for cluster {}.", sdxCluster.getClusterName());
                updateLoadBalancerDNSService.performLoadBalancerDNSUpdateOnIPA(sdxCluster);
                LOGGER.info("Successfully updated the load balancer DNS on Free IPA for cluster {}.", sdxCluster.getClusterName());
                eventSenderService.notifyEvent(sdxCluster, ResourceEvent.UPDATE_LOAD_BALANCER_DNS_IPA_FINISHED);
                sendNotificationInCaseOfResizeOrRecovery(context, sdxCluster);
                sendEvent(context, UPDATE_LOAD_BALANCER_DNS_IPA_SUCCESS_EVENT.event(), payload);
            }

            private void sendNotificationInCaseOfResizeOrRecovery(SdxContext context, SdxCluster sdxCluster) {
                Optional<FlowLogWithoutPayload> flowLog = flowLogService.getLastFlowLog(context.getFlowParameters().getFlowId());
                if (flowChainLogService.isFlowTriggeredByFlowChain(
                        DatalakeResizeRecoveryFlowEventChainFactory.class.getSimpleName(), flowLog)) {
                    eventSenderService.sendEventAndNotification(sdxCluster, ResourceEvent.DATALAKE_RECOVERY_FINISHED);
                } else if (flowChainLogService.isFlowTriggeredByFlowChain(
                        DatalakeResizeFlowEventChainFactory.class.getSimpleName(), flowLog)) {
                    eventSenderService.notifyEvent(context, ResourceEvent.DATALAKE_RESIZE_COMPLETE);
                }
            }

            @Override
            protected Object getFailurePayload(StartUpdateLoadBalancerDNSEvent payload, Optional<SdxContext> flowContext, Exception e) {
                LOGGER.error("Failed to update load balancer DNS of SDX with ID {} on Free IPA.", payload.getResourceId());
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
                        sdxCluster, ResourceEvent.UPDATE_LOAD_BALANCER_DNS_FAILED,
                        Set.of(exception.getMessage())
                );
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
