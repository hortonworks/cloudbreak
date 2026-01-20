package com.sequenceiq.cloudbreak.rotation.flow.subrotation;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.rotation.flow.subrotation.config.SecretSubRotationState;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.config.SecretSubRotationStateSelectors;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.ExecuteSubRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SecretSubRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SubRotationEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SubRotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class SecretSubRotationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretSubRotationActions.class);

    @Inject
    private SecretRotationStatusService secretRotationStatusService;

    @Inject
    private SecretRotationUsageService secretRotationUsageService;

    @Bean(name = "EXECUTE_SUB_ROTATION_STATE")
    public Action<?, ?> executeSubRotationAction() {
        return new AbstractAction<SecretSubRotationState, SecretSubRotationStateSelectors,
                SubRotationFlowContext, SecretSubRotationTriggerEvent>(SecretSubRotationTriggerEvent.class) {

            @Override
            protected SubRotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretSubRotationState, SecretSubRotationStateSelectors> stateContext, SecretSubRotationTriggerEvent payload) {
                return SubRotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(SubRotationFlowContext context, SecretSubRotationTriggerEvent payload, Map<Object, Object> variables) {
                sendEvent(context, ExecuteSubRotationTriggerEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(SecretSubRotationTriggerEvent payload, Optional<SubRotationFlowContext> flowContext, Exception ex) {
                return SubRotationFailedEvent.fromPayload(payload, ex);
            }
        };
    }

    @Bean(name = "SUB_ROTATION_FAILURE_STATE")
    public Action<?, ?> subRotationFailureAction() {
        return new AbstractAction<SecretSubRotationState, SecretSubRotationStateSelectors,
                SubRotationFlowContext, SubRotationFailedEvent>(SubRotationFailedEvent.class) {

            @Override
            protected SubRotationFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretSubRotationState, SecretSubRotationStateSelectors> stateContext, SubRotationFailedEvent payload) {
                return SubRotationFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(SubRotationFlowContext context, SubRotationFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                String message = exception.getMessage();
                String resourceCrn = context.getResourceCrn();
                LOGGER.warn("Secret sub rotation failed for secreType: {}, executionType: {}", context.getSecretType(), context.getExecutionType(), exception);
                if (PREVALIDATE.equals(payload.getExecutionType())) {
                    secretRotationStatusService.preVaildationFailed(resourceCrn, payload.getSecretType(), message);
                } else {
                    secretRotationUsageService.rotationFailed(context.getSecretType(), resourceCrn, message, context.getExecutionType());
                    secretRotationStatusService.rotationFailed(resourceCrn, payload.getSecretType(), message);
                }
                sendEvent(context, SubRotationEvent.fromContext(SecretSubRotationStateSelectors.SUB_ROTATION_FAILURE_HANDLED_EVENT.event(), context));
            }

            @Override
            protected Object getFailurePayload(SubRotationFailedEvent payload, Optional<SubRotationFlowContext> flowContext, Exception ex) {
                LOGGER.error("Secret sub rotation default failure state should not ever fail.");
                return null;
            }
        };
    }

}

