package com.sequenceiq.cloudbreak.rotation.flow.subrotation.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class SecretSubRotationFlowConfig extends AbstractFlowConfiguration<SecretSubRotationState, SecretSubRotationStateSelectors>
        implements FlowConfiguration<SecretSubRotationStateSelectors> {

    private static final List<Transition<SecretSubRotationState, SecretSubRotationStateSelectors>> TRANSITIONS =
            new Transition.Builder<SecretSubRotationState, SecretSubRotationStateSelectors>()
                    .defaultFailureEvent(SecretSubRotationStateSelectors.SUB_ROTATION_FAILED_EVENT)

                    .from(SecretSubRotationState.INIT_STATE)
                    .to(SecretSubRotationState.EXECUTE_SUB_ROTATION_STATE)
                    .event(SecretSubRotationStateSelectors.SECRET_SUB_ROTATION_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(SecretSubRotationState.EXECUTE_SUB_ROTATION_STATE)
                    .to(SecretSubRotationState.FINAL_STATE)
                    .event(SecretSubRotationStateSelectors.SUB_ROTATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SecretSubRotationState, SecretSubRotationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(SecretSubRotationState.INIT_STATE, SecretSubRotationState.FINAL_STATE,
                    SecretSubRotationState.SUB_ROTATION_FAILURE_STATE, SecretSubRotationStateSelectors.SUB_ROTATION_FAILURE_HANDLED_EVENT);

    public SecretSubRotationFlowConfig() {
        super(SecretSubRotationState.class, SecretSubRotationStateSelectors.class);
    }

    @Override
    protected List<Transition<SecretSubRotationState, SecretSubRotationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SecretSubRotationState, SecretSubRotationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SecretSubRotationStateSelectors[] getEvents() {
        return SecretSubRotationStateSelectors.values();
    }

    @Override
    public SecretSubRotationStateSelectors[] getInitEvents() {
        return new SecretSubRotationStateSelectors[]{
                SecretSubRotationStateSelectors.SECRET_SUB_ROTATION_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Secret sub rotation";
    }

}
