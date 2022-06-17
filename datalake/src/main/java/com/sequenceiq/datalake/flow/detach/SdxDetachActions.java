package com.sequenceiq.datalake.flow.detach;

import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_ATTACH_NEW_CLUSTER_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_CLUSTER_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EXTERNAL_DB_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_STACK_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_STACK_SUCCESS_WITH_EXTERNAL_DB_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxDetachFailedEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.attach.SdxAttachService;
import com.sequenceiq.datalake.service.sdx.attach.SdxDetachService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxDetachActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetachActions.class);

    private static final String RESIZED_SDX = "RESIZED_SDX";

    private static final String DETACHED_SDX = "DETACHED_SDX";

    private static final String IS_DETACH_DURING_RECOVERY = "IS_DETACH_DURING_RECOVERY";

    @Inject
    private SdxDetachService sdxDetachService;

    @Inject
    private SdxAttachService sdxAttachService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private EventSenderService eventSenderService;

    @Bean(name = "SDX_DETACH_CLUSTER_STATE")
    public Action<?, ?> sdxDetachCluster() {
        return new AbstractSdxAction<>(SdxStartDetachEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxStartDetachEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartDetachEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.info("Detaching of SDX with ID {} in progress.", payload.getResourceId());
                SdxCluster detached = sdxDetachService.detachCluster(payload.getResourceId(), payload.isDetachDuringRecovery());
                variables.put(RESIZED_SDX, payload.getSdxCluster());
                variables.put(DETACHED_SDX, detached);
                variables.put(IS_DETACH_DURING_RECOVERY, payload.isDetachDuringRecovery());
                eventSenderService.sendEventAndNotification(
                        detached, context.getFlowTriggerUserCrn(), ResourceEvent.SDX_DETACH_STARTED,
                        List.of(detached.getClusterName())
                );
                sendEvent(context, SDX_DETACH_CLUSTER_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxStartDetachEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                return SdxDetachFailedEvent.from(payload, e);
            }
        };
    }

    @Bean(name = "SDX_DETACH_STACK_STATE")
    public Action<?, ?> sdxDetachStack() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                sdxDetachService.detachStack(
                        (SdxCluster) variables.get(DETACHED_SDX),
                        ((SdxCluster) variables.get(RESIZED_SDX)).getClusterName()
                );

                if (((SdxCluster) variables.get(DETACHED_SDX)).hasExternalDatabase()) {
                    sendEvent(context, SDX_DETACH_STACK_SUCCESS_WITH_EXTERNAL_DB_EVENT.event(), payload);
                } else {
                    sendEvent(context, SDX_DETACH_STACK_SUCCESS_EVENT.event(), payload);
                }
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Detach stack Detach action failed!", e);
                return SdxDetachFailedEvent.from(payload, e);
            }
        };
    }

    @Bean(name = "SDX_DETACH_STACK_FAILED_STATE")
    public Action<?, ?> sdxDetachStackFailedAction() {
        return new AbstractSdxAction<>(SdxDetachFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxDetachFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload, Map<Object, Object> variables) {
                if (!((boolean) variables.get(IS_DETACH_DURING_RECOVERY))) {
                    SdxCluster clusterToReattach = (SdxCluster) variables.get(DETACHED_SDX);
                    LOGGER.error("Failed to detach stack of SDX with ID: {}. Attempting to restore it.", clusterToReattach.getId());
                    clusterToReattach = sdxAttachService.reattachCluster(clusterToReattach);
                    LOGGER.info("Successfully restored detached SDX with ID {} which failed to detach its stack.",
                            clusterToReattach.getId());
                }
                sendEvent(context, SDX_DETACH_FAILED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Failed to reattach SDX with ID {} which failed to detach its stack.",
                        payload.getResourceId());
                return payload;
            }
        };
    }

    @Bean(name = "SDX_DETACH_EXTERNAL_DB_STATE")
    public Action<?, ?> sdxDetachExternalDB() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                sdxDetachService.detachExternalDatabase((SdxCluster) variables.get(DETACHED_SDX));
                sendEvent(context, SDX_DETACH_EXTERNAL_DB_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Detach external DB Detach action failed!", e);
                return SdxDetachFailedEvent.from(payload, e);
            }
        };
    }

    @Bean(name = "SDX_DETACH_EXTERNAL_DB_FAILED_STATE")
    public Action<?, ?> sdxDetachExternalDBFailedAction() {
        return new AbstractSdxAction<>(SdxDetachFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxDetachFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload, Map<Object, Object> variables) {
                if (!((boolean) variables.get(IS_DETACH_DURING_RECOVERY))) {
                    SdxCluster detached = (SdxCluster) variables.get(DETACHED_SDX);
                    LOGGER.error("Failed to detach external DB of SDX with ID: {}. Attempting to restore it.", detached.getId());

                    String detachedName = detached.getClusterName();
                    SdxCluster reattached = sdxAttachService.reattachCluster(detached);
                    sdxAttachService.reattachStack(reattached, detachedName);

                    LOGGER.info("Successfully restored detached SDX with ID {} which failed to detach its external database.",
                            reattached.getId());
                }
                sendEvent(context, SDX_DETACH_FAILED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Failed to reattach SDX with ID {} which failed to detach its external database.",
                        payload.getResourceId());
                return payload;
            }
        };
    }

    @Bean(name = "SDX_ATTACH_NEW_CLUSTER_STATE")
    public Action<?, ?> sdxAttachNewCluster() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload,
                    Map<Object, Object> variables) throws Exception {
                SdxCluster detachedCluster = (SdxCluster) variables.get(DETACHED_SDX);
                sdxDetachService.markAsDetached(detachedCluster.getId());
                eventSenderService.notifyEvent(detachedCluster, context, ResourceEvent.SDX_DETACH_FINISHED);
                LOGGER.info("Detaching of SDX with ID {} finished.", detachedCluster.getId());

                if (!((boolean) variables.get(IS_DETACH_DURING_RECOVERY))) {
                    SdxCluster resizedCluster = (SdxCluster) variables.get(RESIZED_SDX);
                    LOGGER.info("Attaching of SDX cluster with ID {} in progress.", resizedCluster.getId());
                    MDCBuilder.buildMdcContext(resizedCluster);
                    resizedCluster = sdxAttachService.saveSdxAndAssignResourceOwnerRole(resizedCluster, context.getFlowTriggerUserCrn());
                    sdxAttachService.markAsAttached(resizedCluster);
                    LOGGER.info("Attaching of SDX cluster with ID {} is complete.", resizedCluster.getId());
                    context.setSdxId(resizedCluster.getId());
                }
                sendEvent(context, SDX_ATTACH_NEW_CLUSTER_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Failed to attach new cluster during detach.", e);
                return SdxDetachFailedEvent.from(payload, e);            }
        };
    }

    @Bean(name = "SDX_ATTACH_NEW_CLUSTER_FAILED_STATE")
    public Action<?, ?> sdxAttachNewClusterFailedAction() {
        return new AbstractSdxAction<>(SdxDetachFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxDetachFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload,
                    Map<Object, Object> variables) throws Exception {
                SdxCluster clusterToReattach = (SdxCluster) variables.get(DETACHED_SDX);
                clusterToReattach = sdxAttachService.reattachDetachedSdxCluster(clusterToReattach);
                LOGGER.info("Successfully restored detached SDX with ID {}.", clusterToReattach.getId());

                sendEvent(context, SDX_DETACH_FAILED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Failed to recover from detach of SDX with ID {}.", payload.getResourceId());
                return payload;
            }
        };
    }

    @Bean(name = "SDX_DETACH_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxDetachFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters,
                    StateContext<FlowState, FlowEvent> stateContext,
                    SdxDetachFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Detaching SDX cluster with ID {} and name {} failed with error {}.",
                        payload.getResourceId(), payload.getSdxName(), exception.getMessage(), exception);
                String statusReason = Optional.ofNullable(exception.getMessage()).orElse("unknown error");
                sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.STOPPED,
                        "SDX detach failed due to: " + statusReason, payload.getResourceId()
                );
                getFlow(context.getFlowParameters().getFlowId()).setFlowFailed(payload.getException());
                sendEvent(context, SDX_DETACH_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Critical error in SdxDetachActions. Failure was not handled correctly.", e);
                return null;
            }
        };
    }
}
