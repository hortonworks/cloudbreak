package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.HIGHSTATE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.HIGHSTATE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.SALT_UPDATE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.SALT_UPDATE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.SALT_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.UPDATE_ORCHESTRATOR_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent.UPDATE_ORCHESTRATOR_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState.RUN_HIGHSTATE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState.SALT_UPDATE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState.SALT_UPDATE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState.UPDATE_ORCHESTRATOR_CONFIG_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState.UPDATE_SALT_STATE_FILES_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class SaltUpdateFlowConfig extends StackStatusFinalizerAbstractFlowConfig<SaltUpdateState, SaltUpdateEvent>
        implements RetryableFlowConfiguration<SaltUpdateEvent> {

    private static final List<Transition<SaltUpdateState, SaltUpdateEvent>> TRANSITIONS =
            new Builder<SaltUpdateState, SaltUpdateEvent>().defaultFailureEvent(SALT_UPDATE_FAILED_EVENT)
                    .from(INIT_STATE)
                    .to(UPDATE_SALT_STATE_FILES_STATE)
                    .event(SALT_UPDATE_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_SALT_STATE_FILES_STATE)
                    .to(UPDATE_ORCHESTRATOR_CONFIG_STATE)
                    .event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)

                    .from(UPDATE_ORCHESTRATOR_CONFIG_STATE)
                    .to(RUN_HIGHSTATE_STATE)
                    .event(UPDATE_ORCHESTRATOR_CONFIG_FINISHED_EVENT)
                    .failureEvent(UPDATE_ORCHESTRATOR_CONFIG_FAILED_EVENT)

                    .from(RUN_HIGHSTATE_STATE)
                    .to(SALT_UPDATE_FINISHED_STATE)
                    .event(HIGHSTATE_FINISHED_EVENT)
                    .failureEvent(HIGHSTATE_FAILED_EVENT)

                    .from(SALT_UPDATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(SALT_UPDATE_FINISHED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SaltUpdateState, SaltUpdateEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SALT_UPDATE_FAILED_STATE, SALT_UPDATE_FAILURE_HANDLED_EVENT);

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
        return new SaltUpdateEvent[]{SALT_UPDATE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Salt update";
    }

    @Override
    public SaltUpdateEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
