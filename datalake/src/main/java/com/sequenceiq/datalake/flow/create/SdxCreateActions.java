package com.sequenceiq.datalake.flow.create;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxCreateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateActions.class);

    @Inject
    private ProvisionerService provisionerService;

    @Inject
    private SdxService sdxService;

    @Bean(name = "SDX_CREATION_START_STATE")
    public Action<?, ?> sdxCreation() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(String flowId, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return new SdxContext(flowId, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                provisionerService.startProvisioning(payload.getResourceId());
                sendEvent(context.getFlowId(), SDX_STACK_CREATION_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "SDX_STACK_CREATION_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackCreationInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(String flowId, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return new SdxContext(flowId, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new StackCreationWaitRequest(context.getSdxId());
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(StackCreationSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(String flowId, StateContext<FlowState, FlowEvent> stateContext, StackCreationSuccessEvent payload) {
                return new SdxContext(flowId, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, StackCreationSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context.getFlowId(), SDX_CREATE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StackCreationSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_CREATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(String flowId, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return new SdxContext(flowId, payload.getResourceId());
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                sdxService.updateSdxStatus(payload.getResourceId(), SdxClusterStatus.PROVISIONING_FAILED);
                sendEvent(context.getFlowId(), SDX_CREATE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

}
