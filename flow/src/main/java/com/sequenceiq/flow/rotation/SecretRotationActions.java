package com.sequenceiq.flow.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException.getFailedStepFromException;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.rotation.config.SecretRotationEvent;
import com.sequenceiq.flow.rotation.config.SecretRotationState;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFailedEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFinishedEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationTriggerEvent;
import com.sequenceiq.flow.rotation.event.FinalizeRotationTriggerEvent;
import com.sequenceiq.flow.rotation.event.RollbackRotationTriggerEvent;
import com.sequenceiq.flow.rotation.event.RotationEvent;
import com.sequenceiq.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.flow.rotation.status.service.SecretRotationStatusService;

@Configuration
public class SecretRotationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationActions.class);

    private static final String EXPLICIT_ROLLBACK_EXECUTION = "Explicit rollback execution.";

    @Inject
    private SecretRotationStatusService secretRotationStatusService;

    @Bean(name = "EXECUTE_ROTATION_STATE")
    public Action<?, ?> executeRotationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationEvent,
                RotationFlowContext, SecretRotationTriggerEvent>(SecretRotationTriggerEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationEvent> stateContext, SecretRotationTriggerEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, SecretRotationTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                if (RotationFlowExecutionType.ROLLBACK.equals(payload.getExecutionType())) {
                    sendEvent(context, ExecuteRotationFailedEvent.fromPayload(payload, new SecretRotationException(EXPLICIT_ROLLBACK_EXECUTION, null), null));
                } else {
                    sendEvent(context, ExecuteRotationTriggerEvent.fromPayload(payload));
                }
            }

            @Override
            protected Object getFailurePayload(SecretRotationTriggerEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
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
                if (RotationFlowExecutionType.ROLLBACK != payload.getExecutionType()
                        || !EXPLICIT_ROLLBACK_EXECUTION.equals(payload.getException().getMessage())) {
                    LOGGER.debug("Execution type is not set or not explicit ROLLBACK, set flow failed for: {}", context.getResourceCrn());
                    flow.setFlowFailed(payload.getException());
                }
                LOGGER.debug("Secret rotation failed, change resource status for {}", context.getResourceCrn());
                secretRotationStatusService.rotationFailed(context.getResourceCrn(), payload.getException().getMessage());
                LOGGER.debug("Secret rotation failed, resource status changed for {}", context.getResourceCrn());
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
