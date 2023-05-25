package com.sequenceiq.flow.rotation.status;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SecretRotationStatusChangeFlowConfig extends AbstractFlowConfiguration<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent>
        implements RetryableFlowConfiguration<SecretRotationStatusChangeEvent> {

    private static final List<Transition<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent>> TRANSITIONS =
            new Transition.Builder<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent>()
                    .defaultFailureEvent(SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_FAILED_EVENT)

                    .from(SecretRotationStatusChangeState.INIT_STATE)
                    .to(SecretRotationStatusChangeState.SECRET_ROTATION_STATUS_CHANGE_STARTED_STATE)
                    .event(SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(SecretRotationStatusChangeState.SECRET_ROTATION_STATUS_CHANGE_STARTED_STATE)
                    .to(SecretRotationStatusChangeState.FINAL_STATE)
                    .event(SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(SecretRotationStatusChangeState.INIT_STATE, SecretRotationStatusChangeState.FINAL_STATE,
                    SecretRotationStatusChangeState.SECRET_ROTATION_STATUS_CHANGE_FAILED_STATE,
                    SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_FAIL_HANDLED_EVENT);

    public SecretRotationStatusChangeFlowConfig() {
        super(SecretRotationStatusChangeState.class, SecretRotationStatusChangeEvent.class);
    }

    @Override
    protected List<Transition<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SecretRotationStatusChangeState, SecretRotationStatusChangeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SecretRotationStatusChangeEvent[] getEvents() {
        return SecretRotationStatusChangeEvent.values();
    }

    @Override
    public SecretRotationStatusChangeEvent[] getInitEvents() {
        return new SecretRotationStatusChangeEvent[]{
                SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Set resource status for secret rotation";
    }

    @Override
    public SecretRotationStatusChangeEvent getRetryableEvent() {
        return SecretRotationStatusChangeEvent.SECRET_ROTATION_STATUS_CHANGE_FAIL_HANDLED_EVENT;
    }
}
