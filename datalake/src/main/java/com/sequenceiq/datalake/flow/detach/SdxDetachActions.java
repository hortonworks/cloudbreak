package com.sequenceiq.datalake.flow.detach;


import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_SUCCESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxDetachSuccessEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxDetachFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.detach.SdxDetachService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxDetachActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetachActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxDetachService sdxDetachService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Bean(name = "SDX_DETACH_START_STATE")
    public Action<?, ?> sdxDetach() {
        return new AbstractSdxAction<>(SdxStartDetachEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxStartDetachEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartDetachEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Execute detach flow for SDX: {}", payload.getResourceId());
                variables.put("SDX", payload.getSdxCluster());
                sdxDetachService.detach(payload.getResourceId());
                sendEvent(context, SDX_DETACH_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxStartDetachEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDetachFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_DETACH_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackDetachInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX detaching in progress: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, "Datalake detaching in progress", payload.getResourceId());
                sendEvent(context, new SdxDetachSuccessEvent(SDX_DETACH_SUCCESS_EVENT.event(), payload.getResourceId(), payload.getUserId()));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDetachFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_DETACH_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(SdxDetachSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxDetachSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDetachSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX detach finalized: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, "Datalake is detached", payload.getResourceId());
                if (variables.containsKey("SDX")) {
                    SdxCluster sdxCluster = (SdxCluster) variables.get("SDX");
                    MDCBuilder.buildMdcContext(sdxCluster);
                    try {
                        transactionService.required(() -> {
                            SdxCluster created = sdxClusterRepository.save(sdxCluster);
                            ownerAssignmentService.assignResourceOwnerRoleIfEntitled(created.getInitiatorUserCrn(), created.getCrn(), created.getAccountId());
                            sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.REQUESTED, "Datalake requested", created);
                            context.setSdxId(created.getId());
                            return created;
                        });
                    } catch (TransactionService.TransactionExecutionException e) {
                        throw new TransactionService.TransactionRuntimeExecutionException(e);
                    }
                }
                sendEvent(context, SDX_DETACH_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_DETACH_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxDetachFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxDetachFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDetachFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                DatalakeStatusEnum failedStatus = DatalakeStatusEnum.STOPPED;
                LOGGER.error("Detaching SDX failed with status {} for resource: {}", failedStatus, payload.getResourceId(), exception);
                String statusReason = "SDX detach failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                sdxStatusService.setStatusForDatalakeAndNotify(failedStatus, statusReason, payload.getResourceId());
                Flow flow = getFlow(context.getFlowParameters().getFlowId());
                flow.setFlowFailed(payload.getException());
                sendEvent(context, SDX_DETACH_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDetachFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
