package com.sequenceiq.datalake.flow.delete;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_STACK_DELETION_IN_PROGRESS_EVENT;

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
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private ProvisionerService provisionerService;

    @Bean(name = "SDX_DELETION_START_STATE")
    public Action<?, ?> sdxDeletion() {
        return new AbstractSdxAction<>(SdxDeleteStartEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxDeleteStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDeleteStartEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                LOGGER.info("Start stack deletion for SDX: {}", payload.getResourceId());
                provisionerService.startStackDeletion(payload.getResourceId(), payload.isForced());
                sendEvent(context, SDX_STACK_DELETION_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDeleteStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDeletionFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_STACK_DELETION_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackDeletionInProgress() {
        return new AbstractSdxAction<>(SdxDeleteStartEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxDeleteStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDeleteStartEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                LOGGER.info("SDX stack deletion in progress: {}", payload.getResourceId());
                sendEvent(context, StackDeletionWaitRequest.from(context, payload));
            }

            @Override
            protected Object getFailurePayload(SdxDeleteStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDeletionFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_DELETION_WAIT_RDS_STATE")
    public Action<?, ?> sdxDeleteRdsAction() {
        return new AbstractSdxAction<>(StackDeletionSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StackDeletionSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StackDeletionSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                LOGGER.info("SDX delete remote database of sdx cluster: {}", payload.getResourceId());
                sendEvent(context, RdsDeletionWaitRequest.from(context, payload));
            }

            @Override
            protected Object getFailurePayload(StackDeletionSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxDeletionFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_DELETION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(RdsDeletionSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    RdsDeletionSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, RdsDeletionSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                LOGGER.info("SDX delete finalized: {}", payload.getResourceId());
                sendEvent(context, SDX_DELETE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(RdsDeletionSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_DELETION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxDeletionFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxDeletionFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxDeletionFailedEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                Exception exception = payload.getException();
                DatalakeStatusEnum deleteFailedStatus = DatalakeStatusEnum.DELETE_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", deleteFailedStatus, payload.getResourceId(), exception);
                String statusReason = "SDX deletion failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                sdxStatusService.setStatusForDatalake(deleteFailedStatus, statusReason, payload.getResourceId());
                sendEvent(context, SDX_DELETE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxDeletionFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
