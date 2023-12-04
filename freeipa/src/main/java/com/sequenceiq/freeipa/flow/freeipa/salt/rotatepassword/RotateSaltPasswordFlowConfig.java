package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import static com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordState.ROTATE_SALT_PASSWORD_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordState.ROTATE_SALT_PASSWORD_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordState.ROTATE_SALT_PASSWORD_SUCCESS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class RotateSaltPasswordFlowConfig extends AbstractFlowConfiguration<RotateSaltPasswordState, RotateSaltPasswordEvent> {
    private static final List<Transition<RotateSaltPasswordState, RotateSaltPasswordEvent>> TRANSITIONS =
            new Transition.Builder<RotateSaltPasswordState, RotateSaltPasswordEvent>()
                    .defaultFailureEvent(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FAILED_EVENT)

                    .from(INIT_STATE).to(ROTATE_SALT_PASSWORD_STATE).event(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_EVENT).noFailureEvent()
                    .from(ROTATE_SALT_PASSWORD_STATE).to(ROTATE_SALT_PASSWORD_SUCCESS_STATE).event(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_SUCCESS_EVENT)
                    .defaultFailureEvent()
                    .from(ROTATE_SALT_PASSWORD_SUCCESS_STATE).to(FINAL_STATE).event(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<RotateSaltPasswordState, RotateSaltPasswordEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ROTATE_SALT_PASSWORD_FAILED_STATE, RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT);

    public RotateSaltPasswordFlowConfig() {
        super(RotateSaltPasswordState.class, RotateSaltPasswordEvent.class);
    }

    @Override
    protected List<Transition<RotateSaltPasswordState, RotateSaltPasswordEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RotateSaltPasswordState, RotateSaltPasswordEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RotateSaltPasswordEvent[] getEvents() {
        return RotateSaltPasswordEvent.values();
    }

    @Override
    public RotateSaltPasswordEvent[] getInitEvents() {
        return new RotateSaltPasswordEvent[]{
                RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Rotate salt password";
    }
}
