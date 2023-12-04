package com.sequenceiq.datalake.flow.salt.update;

import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_WAIT_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateState.INIT_STATE;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateState.SALT_UPDATE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateState.SALT_UPDATE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateState.SALT_UPDATE_STATE;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateState.SALT_UPDATE_WAIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SaltUpdateFlowConfig extends AbstractFlowConfiguration<SaltUpdateState, SaltUpdateEvent> {

    private static final List<Transition<SaltUpdateState, SaltUpdateEvent>> TRANSITIONS =
            new Transition.Builder<SaltUpdateState, SaltUpdateEvent>()
                    .defaultFailureEvent(SALT_UPDATE_FAILED_EVENT)

                    .from(INIT_STATE).to(SALT_UPDATE_STATE)
                    .event(SALT_UPDATE_EVENT)
                    .noFailureEvent()

                    .from(SALT_UPDATE_STATE).to(SALT_UPDATE_WAIT_STATE)
                    .event(SALT_UPDATE_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(SALT_UPDATE_WAIT_STATE).to(SALT_UPDATE_FINISHED_STATE)
                    .event(SALT_UPDATE_WAIT_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(SALT_UPDATE_FINISHED_STATE).to(FINAL_STATE)
                    .event(SALT_UPDATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SaltUpdateState, SaltUpdateEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SALT_UPDATE_FAILED_STATE, SALT_UPDATE_FAIL_HANDLED_EVENT);

    public SaltUpdateFlowConfig() {
        super(SaltUpdateState.class, SaltUpdateEvent.class);
    }

    @Override
    protected List<Transition<SaltUpdateState, SaltUpdateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SaltUpdateState, SaltUpdateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SaltUpdateEvent[] getEvents() {
        return SaltUpdateEvent.values();
    }

    @Override
    public SaltUpdateEvent[] getInitEvents() {
        return new SaltUpdateEvent[]{
                SALT_UPDATE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Run Salt update";
    }
}