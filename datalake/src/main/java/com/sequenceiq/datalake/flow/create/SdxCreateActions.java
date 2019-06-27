package com.sequenceiq.datalake.flow.create;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;

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
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.notification.NotificationService;
import com.sequenceiq.notification.ResourceEvent;

@Configuration
public class SdxCreateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateActions.class);

    @Inject
    private ProvisionerService provisionerService;

    @Inject
    private SdxService sdxService;

    @Inject
    private NotificationService notificationService;

    @Bean(name = "SDX_CREATION_WAIT_ENV_STATE")
    public Action<?, ?> envWaitInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId(), payload.getUserId());
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                notificationService.send(ResourceEvent.SDX_WAITING_FOR_ENVIRONMENT, context.getUserId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new EnvWaitRequest(context.getSdxId(), context.getUserId());
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCreateFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_START_STATE")
    public Action<?, ?> sdxCreation() {
        return new AbstractSdxAction<>(EnvWaitSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    EnvWaitSuccessEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId(), payload.getUserId());
            }

            @Override
            protected void doExecute(SdxContext context, EnvWaitSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                notificationService.send(ResourceEvent.SDX_ENVIRONMENT_FINISHED, payload.getDetailedEnvironmentResponse(), context.getUserId());
                provisionerService.startStackProvisioning(payload.getResourceId(), payload.getDetailedEnvironmentResponse());
                notificationService.send(ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, context.getUserId());

                sendEvent(context, SDX_STACK_CREATION_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(EnvWaitSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCreateFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "SDX_STACK_CREATION_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackCreationInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId(), payload.getUserId());
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new StackCreationWaitRequest(context.getSdxId(), context.getUserId());
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCreateFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(StackCreationSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StackCreationSuccessEvent payload) {
                return new SdxContext(flowParameters, payload.getResourceId(), payload.getUserId());
            }

            @Override
            protected void doExecute(SdxContext context, StackCreationSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                notificationService.send(ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED, context.getUserId());
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
                notificationService.send(ResourceEvent.SDX_CLUSTER_CREATION_FAILED, payload, payload.getUserId());
                return new SdxContext(flowParameters, payload.getResourceId(), null);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCreateFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                SdxClusterStatus provisioningFailedStatus = SdxClusterStatus.PROVISIONING_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", provisioningFailedStatus.name(), payload.getResourceId(), exception);
                String statusReason = ExceptionUtils.getMessage(exception);
                sdxService.updateSdxStatus(payload.getResourceId(), provisioningFailedStatus, statusReason);
                sendEvent(context, SDX_CREATE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxCreateFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

}
