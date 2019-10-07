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
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.RdsWaitRequest;
import com.sequenceiq.datalake.flow.create.event.RdsWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxCreateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateActions.class);

    @Inject
    private ProvisionerService provisionerService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "SDX_CREATION_WAIT_ENV_STATE")
    public Action<?, ?> envWaitInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return EnvWaitRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_WAIT_RDS_STATE")
    public Action<?, ?> rdsCreation() {
        return new AbstractSdxAction<>(EnvWaitSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    EnvWaitSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, EnvWaitSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                RdsWaitRequest req = new RdsWaitRequest(context, payload.getDetailedEnvironmentResponse());
                sendEvent(context, req.selector(), req);
            }

            @Override
            protected Object getFailurePayload(EnvWaitSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_START_STATE")
    public Action<?, ?> sdxCreation() {
        return new AbstractSdxAction<>(RdsWaitSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    RdsWaitSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, RdsWaitSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                provisionerService.startStackProvisioning(payload.getResourceId(),
                        payload.getDetailedEnvironmentResponse(), payload.getDatabaseServerResponse());

                sendEvent(context, SDX_STACK_CREATION_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(RdsWaitSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_STACK_CREATION_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackCreationInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return StackCreationWaitRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(StackCreationSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StackCreationSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StackCreationSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SDX_CLUSTER_CREATED,
                        "Datalake is running", payload.getResourceId());
                sendEvent(context, SDX_CREATE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StackCreationSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_CREATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxCreateFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxCreateFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCreateFailedEvent payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.addRequestId(context.getRequestId());
                Exception exception = payload.getException();
                LOGGER.error("Datalake create failed for datalakeId: {}", payload.getResourceId(), exception);
                String statusReason = "Datalake creation failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.PROVISIONING_FAILED, ResourceEvent.SDX_CLUSTER_CREATION_FAILED,
                        statusReason, payload.getResourceId());
                sendEvent(context, SDX_CREATE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxCreateFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

}
