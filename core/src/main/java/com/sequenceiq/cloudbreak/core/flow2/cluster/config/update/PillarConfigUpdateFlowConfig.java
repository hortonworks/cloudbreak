package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState.PILLAR_CONFIG_UPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState.PILLAR_CONFIG_UPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState.PILLAR_CONFIG_UPDATE_START_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_FINISHED_EVENT;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;

@Component
public class PillarConfigUpdateFlowConfig extends
    AbstractFlowConfiguration<PillarConfigUpdateState, PillarConfigurationUpdateEvent> {

    private static final List<Transition<PillarConfigUpdateState, PillarConfigurationUpdateEvent>> TRANSITIONS =
        new Builder<PillarConfigUpdateState, PillarConfigurationUpdateEvent>()
            .defaultFailureEvent(PILLAR_CONFIG_UPDATE_FAILED_EVENT)
            .from(INIT_STATE).to(PILLAR_CONFIG_UPDATE_START_STATE).event(PILLAR_CONFIG_UPDATE_EVENT)
            .noFailureEvent()
            .from(PILLAR_CONFIG_UPDATE_START_STATE).to(PILLAR_CONFIG_UPDATE_FINISHED_STATE)
            .event(PILLAR_CONFIG_UPDATE_FINISHED_EVENT).failureEvent(
            PILLAR_CONFIG_UPDATE_FAILED_EVENT)
            .from(PILLAR_CONFIG_UPDATE_FINISHED_STATE).to(FINAL_STATE).event(
            PILLAR_CONFIG_UPDATE_FINALIZE_EVENT)
            .noFailureEvent()
            .build();

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public PillarConfigUpdateFlowConfig() {
        super(PillarConfigUpdateState.class, PillarConfigurationUpdateEvent.class);
    }

    @Override
    protected List<Transition<PillarConfigUpdateState, PillarConfigurationUpdateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<PillarConfigUpdateState, PillarConfigurationUpdateEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, PILLAR_CONFIG_UPDATE_FAILED_STATE,
            PILLAR_CONFIG_UPDATE_FAILURE_HANDLED_EVENT);
    }

    @Override
    public PillarConfigurationUpdateEvent[] getEvents() {
        return PillarConfigurationUpdateEvent.values();
    }

    @Override
    public PillarConfigurationUpdateEvent[] getInitEvents() {
        return new PillarConfigurationUpdateEvent[]{PILLAR_CONFIG_UPDATE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Pillar configuration update";
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
