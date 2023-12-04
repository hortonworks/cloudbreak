package com.sequenceiq.datalake.flow.salt.rotatepassword;

import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerEvent.ROTATE_SALT_PASSWORD_EVENT;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerEvent.ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerEvent.ROTATE_SALT_PASSWORD_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerEvent.ROTATE_SALT_PASSWORD_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerState.INIT_STATE;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerState.ROTATE_SALT_PASSWORD_FAILED_STATE;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerState.ROTATE_SALT_PASSWORD_SUCCESS_STATE;
import static com.sequenceiq.datalake.flow.salt.rotatepassword.RotateSaltPasswordTrackerState.ROTATE_SALT_PASSWORD_WAITING_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class RotateSaltPasswordTrackerFlowConfig extends AbstractFlowConfiguration<RotateSaltPasswordTrackerState, RotateSaltPasswordTrackerEvent> {

    private static final List<Transition<RotateSaltPasswordTrackerState, RotateSaltPasswordTrackerEvent>> TRANSITIONS =
            new Transition.Builder<RotateSaltPasswordTrackerState, RotateSaltPasswordTrackerEvent>()
                    .defaultFailureEvent(RotateSaltPasswordTrackerEvent.ROTATE_SALT_PASSWORD_FAILED_EVENT)

                    .from(INIT_STATE).to(ROTATE_SALT_PASSWORD_WAITING_STATE).event(ROTATE_SALT_PASSWORD_EVENT).noFailureEvent()
                    .from(ROTATE_SALT_PASSWORD_WAITING_STATE).to(ROTATE_SALT_PASSWORD_SUCCESS_STATE).event(ROTATE_SALT_PASSWORD_SUCCESS_EVENT)
                    .defaultFailureEvent()
                    .from(ROTATE_SALT_PASSWORD_SUCCESS_STATE).to(FINAL_STATE).event(ROTATE_SALT_PASSWORD_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<RotateSaltPasswordTrackerState, RotateSaltPasswordTrackerEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ROTATE_SALT_PASSWORD_FAILED_STATE, ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT);

    public RotateSaltPasswordTrackerFlowConfig() {
        super(RotateSaltPasswordTrackerState.class, RotateSaltPasswordTrackerEvent.class);
    }

    @Override
    protected List<Transition<RotateSaltPasswordTrackerState, RotateSaltPasswordTrackerEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RotateSaltPasswordTrackerState, RotateSaltPasswordTrackerEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RotateSaltPasswordTrackerEvent[] getEvents() {
        return RotateSaltPasswordTrackerEvent.values();
    }

    @Override
    public RotateSaltPasswordTrackerEvent[] getInitEvents() {
        return new RotateSaltPasswordTrackerEvent[]{
                ROTATE_SALT_PASSWORD_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Rotate SaltStack user password";
    }
}
