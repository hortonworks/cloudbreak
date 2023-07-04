package com.sequenceiq.cloudbreak.rotation.flow.rotation;

import static com.sequenceiq.cloudbreak.rotation.common.SecretRotationException.getFailedStepFromException;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.config.SecretRotationEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.config.SecretRotationState;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.FinalizeRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.PreValidateRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.PreValidateRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RollbackRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class SecretRotationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationActions.class);

    private static final String EXPLICIT_ROLLBACK_EXECUTION = "Explicit rollback execution.";

    @Inject
    private SecretRotationStatusService secretRotationStatusService;

    @Inject
    private SecretRotationUsageService secretRotationUsageService;

    @Inject
    private SecretRotationStepProgressService secretRotationProgressService;

    @Bean(name = "PRE_VALIDATE_ROTATION_STATE")
    public Action<?, ?> executePreValidationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationEvent,
                RotationFlowContext, SecretRotationTriggerEvent>(SecretRotationTriggerEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationEvent> stateContext, SecretRotationTriggerEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, SecretRotationTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context, PreValidateRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(SecretRotationTriggerEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                SecretRotationStep failedStep = getFailedStepFromException(ex);
                return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), payload, ex, failedStep);
            }
        };
    }

    @Bean(name = "EXECUTE_ROTATION_STATE")
    public Action<?, ?> executeRotationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationEvent,
                RotationFlowContext, PreValidateRotationFinishedEvent>(PreValidateRotationFinishedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationEvent> stateContext, PreValidateRotationFinishedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, PreValidateRotationFinishedEvent payload, Map<Object, Object> variables) throws Exception {
                if (RotationFlowExecutionType.ROLLBACK.equals(payload.getExecutionType())) {
                    LOGGER.info("Routing execution of rotation flow to rollback state, since execution type is specified for secret {} and resource {}",
                            payload.getSecretType(), payload.getResourceCrn());
                    sendEvent(context, ExecuteRotationFailedEvent.fromPayload(payload, new SecretRotationException(EXPLICIT_ROLLBACK_EXECUTION, null), null));
                } else {
                    sendEvent(context, ExecuteRotationTriggerEvent.fromPayload(payload));
                }
            }

            @Override
            protected Object getFailurePayload(PreValidateRotationFinishedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                SecretRotationStep failedStep = getFailedStepFromException(ex);
                return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), payload, ex, failedStep);
            }
        };
    }

    @Bean(name = "FINALIZE_ROTATION_STATE")
    public Action<?, ?> finalizeRotationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationEvent,
                RotationFlowContext, ExecuteRotationFinishedEvent>(ExecuteRotationFinishedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationEvent> stateContext, ExecuteRotationFinishedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, ExecuteRotationFinishedEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context, FinalizeRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(ExecuteRotationFinishedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                SecretRotationStep failedStep = getFailedStepFromException(ex);
                return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), payload, ex, failedStep);
            }
        };
    }

    @Bean(name = "ROLLBACK_ROTATION_STATE")
    public Action<?, ?> rollbackRotationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationEvent,
                RotationFlowContext, ExecuteRotationFailedEvent>(ExecuteRotationFailedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationEvent> stateContext, ExecuteRotationFailedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, ExecuteRotationFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Flow flow = getFlow(context.getFlowId());
                if (RotationFlowExecutionType.ROLLBACK != payload.getExecutionType()) {
                    LOGGER.debug("Execution type is not set or not explicit ROLLBACK, set flow failed for: {}", context.getResourceCrn());
                    flow.setFlowFailed(payload.getException());
                }
                sendEvent(context, RollbackRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(ExecuteRotationFailedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                return RotationFailedEvent.fromPayload(SecretRotationEvent.ROTATION_FAILED_EVENT.event(), payload, ex, payload.getFailedStep());
            }
        };
    }

    @Bean(name = "ROTATION_DEFAULT_FAILURE_STATE")
    public Action<?, ?> rotationFailureAction() {
        return new AbstractAction<SecretRotationState, SecretRotationEvent,
                RotationFlowContext, RotationFailedEvent>(RotationFailedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationEvent> stateContext, RotationFailedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, RotationFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Flow flow = getFlow(context.getFlowId());
                String resourceCrn = context.getResourceCrn();
                String message = payload.getException().getMessage();
                if (RotationFlowExecutionType.ROLLBACK == payload.getExecutionType() && EXPLICIT_ROLLBACK_EXECUTION.equals(message)) {
                    // if the execution tpye is ROLLBACK, we need to know from the calling service wheter it was successful or not
                    LOGGER.debug("Explicit rollback, doesnt set flow failed.");
                } else {
                    LOGGER.debug("Execution type is not set or not explicit ROLLBACK, set flow failed for: {}", context.getResourceCrn());
                    flow.setFlowFailed(payload.getException());
                }
                secretRotationProgressService.deleteAll(context.getResourceCrn(), payload.getSecretType());
                secretRotationUsageService.rotationFailed(context.getSecretType(), resourceCrn, message, context.getExecutionType());
                LOGGER.debug("Secret rotation failed, change resource status for {}", resourceCrn);
                secretRotationStatusService.rotationFailed(resourceCrn, message);
                LOGGER.debug("Secret rotation failed, resource status changed for {}", resourceCrn);
                sendEvent(context, RotationEvent.fromContext(SecretRotationEvent.ROTATION_FAILURE_HANDLED_EVENT.event(), context));
            }

            @Override
            protected Object getFailurePayload(RotationFailedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                LOGGER.error("Secret rotation default failure state should not ever fail.");
                return null;
            }
        };
    }

}
