package com.sequenceiq.flow.rotation.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SecretRotationFlowConfig extends AbstractFlowConfiguration<SecretRotationState, SecretRotationEvent>
        implements RetryableFlowConfiguration<SecretRotationEvent> {

    private static final List<Transition<SecretRotationState, SecretRotationEvent>> TRANSITIONS =
            new Transition.Builder<SecretRotationState, SecretRotationEvent>()
                    .defaultFailureEvent(SecretRotationEvent.ROTATION_FAILED_EVENT)

                    .from(SecretRotationState.INIT_STATE)
                    .to(SecretRotationState.PRE_VALIDATE_ROTATION_STATE)
                    .event(SecretRotationEvent.SECRET_ROTATION_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(SecretRotationState.PRE_VALIDATE_ROTATION_STATE)
                    .to(SecretRotationState.EXECUTE_ROTATION_STATE)
                    .event(SecretRotationEvent.PRE_VALIDATE_ROTATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SecretRotationState.EXECUTE_ROTATION_STATE)
                    .to(SecretRotationState.FINALIZE_ROTATION_STATE)
                    .event(SecretRotationEvent.EXECUTE_ROTATION_FINISHED_EVENT)
                    .failureState(SecretRotationState.ROLLBACK_ROTATION_STATE)
                    .failureEvent(SecretRotationEvent.EXECUTE_ROTATION_FAILED_EVENT)

                    .from(SecretRotationState.ROLLBACK_ROTATION_STATE)
                    .to(SecretRotationState.ROTATION_DEFAULT_FAILURE_STATE)
                    .event(SecretRotationEvent.ROTATION_FAILED_EVENT)
                    .noFailureEvent()

                    .from(SecretRotationState.FINALIZE_ROTATION_STATE)
                    .to(SecretRotationState.FINAL_STATE)
                    .event(SecretRotationEvent.ROTATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SecretRotationState, SecretRotationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(SecretRotationState.INIT_STATE, SecretRotationState.FINAL_STATE,
                    SecretRotationState.ROTATION_DEFAULT_FAILURE_STATE, SecretRotationEvent.ROTATION_FAILURE_HANDLED_EVENT);

    public SecretRotationFlowConfig() {
        super(SecretRotationState.class, SecretRotationEvent.class);
    }

    @Override
    protected List<Transition<SecretRotationState, SecretRotationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SecretRotationState, SecretRotationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SecretRotationEvent[] getEvents() {
        return SecretRotationEvent.values();
    }

    @Override
    public SecretRotationEvent[] getInitEvents() {
        return new SecretRotationEvent[]{
                SecretRotationEvent.SECRET_ROTATION_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Rotation specific secret";
    }

    @Override
    public SecretRotationEvent getRetryableEvent() {
        return SecretRotationEvent.ROTATION_FAILURE_HANDLED_EVENT;
    }
}
