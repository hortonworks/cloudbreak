package com.sequenceiq.datalake.flow.delete;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_STACK_DELETION_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private ProvisionerService provisionerService;

    @Bean(name = "SDX_DELETION_START_STATE")
    public Action<?, ?> sdxDeletion() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Start stack deletion for SDX: {}", payload.getResourceId());
                provisionerService.startStackDeletion(payload.getResourceId());
                sendEvent(context, SDX_STACK_DELETION_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new StackDeletionFailedEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "SDX_STACK_DELETION_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackDeletionInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX stack deletion in progress: {}", payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new StackDeletionWaitRequest(context.getSdxId());
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new StackDeletionFailedEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "SDX_DELETION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(StackDeletionSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StackDeletionSuccessEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, StackDeletionSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("SDX delete finalized: {}", payload.getResourceId());
                sendEvent(context, SDX_DELETE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StackDeletionSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_DELETION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(StackDeletionFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StackDeletionFailedEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, StackDeletionFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                SdxClusterStatus deleteFailedStatus = SdxClusterStatus.DELETE_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", deleteFailedStatus, payload.getResourceId(), exception);
                String statusReason = ExceptionUtils.getMessage(exception);
                sdxService.updateSdxStatus(payload.getResourceId(), deleteFailedStatus, statusReason);
                sendEvent(context, SDX_DELETE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StackDeletionFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
