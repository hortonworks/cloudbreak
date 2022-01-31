package com.sequenceiq.datalake.flow.detach;

import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_ATTACH_NEW_CLUSTER_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_CLUSTER_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EXTERNAL_DB_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_STACK_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_STACK_SUCCESS_WITH_EXTERNAL_DB_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxDetachFailedEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
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

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxDetachService sdxDetachService;

    @Inject
    private SdxAttachService sdxAttachService;

    @Inject
    private SdxStatusService sdxStatusService;

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
                    Map<Object, Object> variables) throws Exception {
                LOGGER.info("Detaching of SDX with ID {} in progress.", payload.getResourceId());
                variables.put(RESIZED_SDX, payload.getSdxCluster());
                variables.put(DETACHED_SDX, sdxDetachService.detachCluster(payload.getResourceId()));
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
            protected void doExecute(SdxContext context, SdxEvent payload,
                    Map<Object, Object> variables) throws Exception {
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
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload,
                    Map<Object, Object> variables) throws Exception {
                SdxCluster detached = (SdxCluster) variables.get(DETACHED_SDX);
                LOGGER.error("Failed to detach stack of SDX with ID: {}. Attempting to restore it.", detached.getId());

                detached = sdxAttachService.reattachCluster(detached);

                LOGGER.info("Successfully restored detached SDX with ID {} which failed to detach its stack.",
                        detached.getId());
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
            protected void doExecute(SdxContext context, SdxEvent payload,
                    Map<Object, Object> variables) throws Exception {
                sdxDetachService.detachExternalDatabase((SdxCluster) variables.get(DETACHED_SDX));
                sendEvent(context, SDX_DETACH_EXTERNAL_DB_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
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
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload,
                    Map<Object, Object> variables) throws Exception {
                SdxCluster detached = (SdxCluster) variables.get(DETACHED_SDX);
                LOGGER.error("Failed to detach external DB of SDX with ID: {}. Attempting to restore it.", detached.getId());

                String detachedName = detached.getClusterName();
                detached = sdxAttachService.reattachCluster(detached);
                sdxAttachService.reattachStack(detached, detachedName);

                LOGGER.info("Successfully restored detached SDX with ID {} which failed to detach its external database.",
                        detached.getId());
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
                LOGGER.info("Detaching of SDX with ID {} finished.", detachedCluster.getId());

                SdxCluster resizedCluster = (SdxCluster) variables.get(RESIZED_SDX);
                LOGGER.info("Attaching of SDX cluster with ID {} in progress.", resizedCluster.getId());
                MDCBuilder.buildMdcContext(resizedCluster);
                resizedCluster = sdxAttachService.saveSdxAndAssignResourceOwnerRole(resizedCluster);
                sdxAttachService.markAsAttached(resizedCluster);
                LOGGER.info("Attaching of SDX cluster with ID {} is complete.", resizedCluster.getId());
                context.setSdxId(resizedCluster.getId());
                sendEvent(context, SDX_ATTACH_NEW_CLUSTER_SUCCESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                return SdxDetachFailedEvent.from(payload, e);
            }
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
                SdxCluster resized = (SdxCluster) variables.get(RESIZED_SDX);
                SdxCluster detached = (SdxCluster) variables.get(DETACHED_SDX);
                LOGGER.error("Failed to attach new cluster with ID {} during detaching of cluster with ID {}." +
                        " Attempting to reattach detached cluster.", resized.getId(), detached.getId());

                detached = sdxAttachService.reattachDetachedSdxCluster(detached);

                LOGGER.info("Successfully restored detached SDX with ID {} which failed during attaching of new cluster.",
                        detached.getId());
                sendEvent(context, SDX_DETACH_FAILED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                LOGGER.error("Failed to reattach SDX with ID {} which failed during attaching of new cluster.",
                        payload.getResourceId());
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
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload,
                    Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                LOGGER.error("Detaching SDX cluster with ID {} and name {} failed with error {}.",
                        payload.getResourceId(), payload.getSdxName(), exception.getMessage(), exception);

                String statusReason = "SDX detach failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, statusReason, payload.getResourceId());

                getFlow(context.getFlowParameters().getFlowId()).setFlowFailed(payload.getException());
                sendEvent(context, SDX_DETACH_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachFailedEvent payload, Optional<SdxContext> flowContext,
                    Exception e) {
                return null;
            }
        };
    }
}
