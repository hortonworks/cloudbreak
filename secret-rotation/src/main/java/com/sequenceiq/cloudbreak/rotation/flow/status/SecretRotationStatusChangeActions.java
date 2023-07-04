package com.sequenceiq.cloudbreak.rotation.flow.status;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.rotation.flow.status.event.RotationStatusChangeFailHandledEvent;
import com.sequenceiq.cloudbreak.rotation.flow.status.event.RotationStatusChangeFailedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.status.event.RotationStatusChangeFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.status.event.RotationStatusChangeTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class SecretRotationStatusChangeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationStatusChangeActions.class);

    @Inject
    private SecretRotationStatusService secretRotationStatusService;

    @Bean(name = "SECRET_ROTATION_STATUS_CHANGE_STARTED_STATE")
    public Action<?, ?> secretRotationStatusChangeAction() {
        return new AbstractAction<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent,
                SecretRotationStatusChangeFlowContext, RotationStatusChangeTriggerEvent>(RotationStatusChangeTriggerEvent.class) {

            @Override
            protected SecretRotationStatusChangeFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent> stateContext, RotationStatusChangeTriggerEvent payload) {
                return SecretRotationStatusChangeFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(SecretRotationStatusChangeFlowContext context, RotationStatusChangeTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Change resource status {} secret rotation for {}", context.isStart() ? "before" : "after", context.getResourceCrn());
                if (context.isStart()) {
                    secretRotationStatusService.rotationStarted(context.getResourceCrn());
                } else {
                    secretRotationStatusService.rotationFinished(context.getResourceCrn());
                }
                LOGGER.debug("Resource status changed {} secret rotation for {}", context.isStart() ? "before" : "after", context.getResourceCrn());
                sendEvent(context, RotationStatusChangeFinishedEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(RotationStatusChangeTriggerEvent payload, Optional<SecretRotationStatusChangeFlowContext> flowContext,
                    Exception ex) {
                return RotationStatusChangeFailedEvent.fromPayload(payload, ex);
            }
        };
    }

    @Bean(name = "SECRET_ROTATION_STATUS_CHANGE_FAILED_STATE")
    public Action<?, ?> secretRotationStatusChangeFailedAction() {
        return new AbstractAction<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent,
                SecretRotationStatusChangeFlowContext, RotationStatusChangeFailedEvent>(RotationStatusChangeFailedEvent.class) {

            @Override
            protected SecretRotationStatusChangeFlowContext createFlowContext(FlowParameters flowParameters,
                    StateContext<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent> stateContext, RotationStatusChangeFailedEvent payload) {
                return SecretRotationStatusChangeFlowContext.fromPayload(flowParameters, payload);
            }

            @Override
            protected void doExecute(SecretRotationStatusChangeFlowContext context, RotationStatusChangeFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Resource status change failed {} secret rotation for {}",
                        context.isStart() ? "before" : "after", context.getResourceCrn(), payload.getException());
                Flow flow = getFlow(context.getFlowId());
                flow.setFlowFailed(payload.getException());
                secretRotationStatusService.rotationFailed(context.getResourceCrn(), "Secret rotation status change failed");
                sendEvent(context, RotationStatusChangeFailHandledEvent.fromPayload(payload));
            }

            @Override
            protected Object getFailurePayload(RotationStatusChangeFailedEvent payload, Optional<SecretRotationStatusChangeFlowContext> flowContext,
                    Exception ex) {
                LOGGER.error("Secret rotation status change default failure state should never fail.");
                return null;
            }
        };
    }
}
