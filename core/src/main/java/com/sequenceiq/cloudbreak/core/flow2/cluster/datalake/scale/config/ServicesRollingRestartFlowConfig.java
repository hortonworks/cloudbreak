package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartState.ROLLING_RESTART_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartState.ROLLING_RESTART_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartState.ROLLING_RESTART_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ServicesRollingRestartFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ServicesRollingRestartState, ServicesRollingRestartEvent>
        implements RetryableFlowConfiguration<ServicesRollingRestartEvent> {

    private static final List<Transition<ServicesRollingRestartState, ServicesRollingRestartEvent>> TRANSITIONS =
            new Builder<ServicesRollingRestartState, ServicesRollingRestartEvent>()
                    .defaultFailureEvent(ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(ROLLING_RESTART_STATE)
                    .event(ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(ROLLING_RESTART_STATE)
                    .to(ROLLING_RESTART_FINISHED_STATE)
                    .event(ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_RESTART_STATE)
                    .to(ROLLING_RESTART_FAILED_STATE)
                    .event(ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FAILURE_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_RESTART_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(ServicesRollingRestartEvent.FINALIZED_EVENT)
                    .noFailureEvent()

                    .from(ROLLING_RESTART_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(ServicesRollingRestartEvent.FINALIZED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ServicesRollingRestartState, ServicesRollingRestartEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            ROLLING_RESTART_FAILED_STATE, ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FAILURE_EVENT);

    protected ServicesRollingRestartFlowConfig() {
        super(ServicesRollingRestartState.class, ServicesRollingRestartEvent.class);
    }

    @Override
    protected List<Transition<ServicesRollingRestartState, ServicesRollingRestartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ServicesRollingRestartState, ServicesRollingRestartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ServicesRollingRestartEvent[] getEvents() {
        return ServicesRollingRestartEvent.values();
    }

    @Override
    public ServicesRollingRestartEvent[] getInitEvents() {
        return new ServicesRollingRestartEvent[]{ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "CM Rolling restart ";
    }

    @Override
    public ServicesRollingRestartEvent getRetryableEvent() {
        return ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FAILURE_EVENT;
    }
}
