package com.sequenceiq.cloudbreak.rotation.flow.rotation.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SecretRotationFlowConfig extends AbstractFlowConfiguration<SecretRotationState, SecretRotationStateSelectors>
        implements RetryableFlowConfiguration<SecretRotationStateSelectors> {

    private static final List<Transition<SecretRotationState, SecretRotationStateSelectors>> TRANSITIONS =
            new Transition.Builder<SecretRotationState, SecretRotationStateSelectors>()
                    .defaultFailureEvent(SecretRotationStateSelectors.ROTATION_FAILED_EVENT)

                    .from(SecretRotationState.INIT_STATE)
                    .to(SecretRotationState.PRE_VALIDATE_ROTATION_STATE)
                    .event(SecretRotationStateSelectors.SECRET_ROTATION_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(SecretRotationState.PRE_VALIDATE_ROTATION_STATE)
                    .to(SecretRotationState.EXECUTE_ROTATION_STATE)
                    .event(SecretRotationStateSelectors.PRE_VALIDATE_ROTATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SecretRotationState.EXECUTE_ROTATION_STATE)
                    .to(SecretRotationState.FINALIZE_ROTATION_STATE)
                    .event(SecretRotationStateSelectors.EXECUTE_ROTATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SecretRotationState.EXECUTE_ROTATION_STATE)
                    .to(SecretRotationState.ROLLBACK_ROTATION_STATE)
                    .event(SecretRotationStateSelectors.EXECUTE_ROTATION_FAILED_EVENT)
                    .defaultFailureEvent()

                    .from(SecretRotationState.ROLLBACK_ROTATION_STATE)
                    .to(SecretRotationState.FINAL_STATE)
                    .event(SecretRotationStateSelectors.EXECUTE_ROLLBACK_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SecretRotationState.FINALIZE_ROTATION_STATE)
                    .to(SecretRotationState.FINAL_STATE)
                    .event(SecretRotationStateSelectors.ROTATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SecretRotationState, SecretRotationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(SecretRotationState.INIT_STATE, SecretRotationState.FINAL_STATE,
                    SecretRotationState.ROTATION_DEFAULT_FAILURE_STATE, SecretRotationStateSelectors.ROTATION_FAILURE_HANDLED_EVENT);

    public SecretRotationFlowConfig() {
        super(SecretRotationState.class, SecretRotationStateSelectors.class);
    }

    @Override
    protected List<Transition<SecretRotationState, SecretRotationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SecretRotationState, SecretRotationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SecretRotationStateSelectors[] getEvents() {
        return SecretRotationStateSelectors.values();
    }

    @Override
    public SecretRotationStateSelectors[] getInitEvents() {
        return new SecretRotationStateSelectors[]{
                SecretRotationStateSelectors.SECRET_ROTATION_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Rotation specific secret";
    }

    @Override
    public SecretRotationStateSelectors getRetryableEvent() {
        return SecretRotationStateSelectors.ROTATION_FAILURE_HANDLED_EVENT;
    }
}
