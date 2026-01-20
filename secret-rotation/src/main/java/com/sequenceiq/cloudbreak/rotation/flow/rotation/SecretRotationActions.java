package com.sequenceiq.cloudbreak.rotation.flow.rotation;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.common.RotationPollerExternalSvcOutageException;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.config.SecretRotationState;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.config.SecretRotationStateSelectors;
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
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class SecretRotationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationActions.class);

    @Inject
    private SecretRotationStatusService secretRotationStatusService;

    @Inject
    private SecretRotationUsageService secretRotationUsageService;

    @Bean(name = "PRE_VALIDATE_ROTATION_STATE")
    public Action<?, ?> executePreValidationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationStateSelectors,
                RotationFlowContext, SecretRotationTriggerEvent>(SecretRotationTriggerEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationStateSelectors> stateContext, SecretRotationTriggerEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, SecretRotationTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context, PreValidateRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(SecretRotationTriggerEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                return RotationFailedEvent.fromPayload(payload, ex, PREVALIDATE);
            }
        };
    }

    @Bean(name = "EXECUTE_ROTATION_STATE")
    public Action<?, ?> executeRotationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationStateSelectors,
                RotationFlowContext, PreValidateRotationFinishedEvent>(PreValidateRotationFinishedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationStateSelectors> stateContext, PreValidateRotationFinishedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, PreValidateRotationFinishedEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context, ExecuteRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(PreValidateRotationFinishedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                if (ex.getCause() instanceof RotationPollerExternalSvcOutageException) {
                    return RotationFailedEvent.fromPayload(payload, ex, ROTATE);
                }
                return ExecuteRotationFailedEvent.fromPayload(payload, ex);
            }
        };
    }

    @Bean(name = "FINALIZE_ROTATION_STATE")
    public Action<?, ?> finalizeRotationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationStateSelectors,
                RotationFlowContext, ExecuteRotationFinishedEvent>(ExecuteRotationFinishedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationStateSelectors> stateContext, ExecuteRotationFinishedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, ExecuteRotationFinishedEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context, FinalizeRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(ExecuteRotationFinishedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                return RotationFailedEvent.fromPayload(payload, ex, FINALIZE);
            }
        };
    }

    @Bean(name = "ROLLBACK_ROTATION_STATE")
    public Action<?, ?> rollbackRotationAction() {
        return new AbstractAction<SecretRotationState, SecretRotationStateSelectors,
                RotationFlowContext, ExecuteRotationFailedEvent>(ExecuteRotationFailedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationStateSelectors> stateContext, ExecuteRotationFailedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, ExecuteRotationFailedEvent payload, Map<Object, Object> variables) {
                sendEvent(context, RollbackRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(ExecuteRotationFailedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                return RotationFailedEvent.fromPayload(payload, ex, ROLLBACK);
            }
        };
    }

    @Bean(name = "ROTATION_DEFAULT_FAILURE_STATE")
    public Action<?, ?> rotationFailureAction() {
        return new AbstractAction<SecretRotationState, SecretRotationStateSelectors,
                RotationFlowContext, RotationFailedEvent>(RotationFailedEvent.class) {

            @Override
            protected RotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationState, SecretRotationStateSelectors> stateContext, RotationFailedEvent payload) {
                return RotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(RotationFlowContext context, RotationFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                RotationFlowExecutionType failedAt = payload.getFailedAt();
                String message = exception.getMessage();
                String resourceCrn = context.getResourceCrn();
                LOGGER.warn("Secret rotation failed for secreType: {}", context.getSecretType(), exception);
                if (PREVALIDATE.equals(failedAt)) {
                    secretRotationStatusService.preVaildationFailed(resourceCrn, payload.getSecretType(), message);
                } else {
                    secretRotationUsageService.rotationFailed(context.getSecretType(), resourceCrn, message, context.getExecutionType());
                    secretRotationStatusService.rotationFailed(resourceCrn, payload.getSecretType(), message);
                }
                sendEvent(context, RotationEvent.fromContext(SecretRotationStateSelectors.ROTATION_FAILURE_HANDLED_EVENT.event(), context));
            }

            @Override
            protected Object getFailurePayload(RotationFailedEvent payload, Optional<RotationFlowContext> flowContext, Exception ex) {
                LOGGER.error("Secret rotation default failure state should not ever fail.");
                return null;
            }
        };
    }

}
