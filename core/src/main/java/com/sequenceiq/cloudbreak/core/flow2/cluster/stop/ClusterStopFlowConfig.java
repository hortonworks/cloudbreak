package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.CLUSTER_AND_STACK_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.CLUSTER_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.CLUSTER_STOP_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.CLUSTER_STOP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.INIT_STATE;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterStopFlowConfig extends AbstractFlowConfiguration<ClusterStopState, ClusterStopEvent> {
    private static final List<Transition<ClusterStopState, ClusterStopEvent>> TRANSITIONS =
            new Transition.Builder<ClusterStopState, ClusterStopEvent>()
                    .from(INIT_STATE).to(CLUSTER_STOP_STATE).event(CLUSTER_STOP_EVENT).noFailureEvent()
                    .from(INIT_STATE).to(CLUSTER_STOP_STATE).event(CLUSTER_AND_STACK_STOP_EVENT).noFailureEvent()
                    .from(CLUSTER_STOP_STATE).to(ClusterStopState.CLUSTER_STOP_FINISHED_STATE).event(ClusterStopEvent.CLUSTER_STOP_FINISHED_EVENT)
                            .failureEvent(ClusterStopEvent.CLUSTER_STOP_FINISHED_FAILURE_EVENT)
                    .from(ClusterStopState.CLUSTER_STOP_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).noFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ClusterStopState, ClusterStopEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_STOP_FAILED_STATE, FAIL_HANDLED_EVENT);

    private static final EnumSet<ClusterStopEvent> OWNEVENTS = EnumSet.complementOf(EnumSet.of(CLUSTER_AND_STACK_STOP_EVENT));

    public ClusterStopFlowConfig() {
        super(ClusterStopState.class, ClusterStopEvent.class);
    }

    @Override

    protected List<Transition<ClusterStopState, ClusterStopEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected AbstractFlowConfiguration.FlowEdgeConfig<ClusterStopState, ClusterStopEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterStopEvent[] getEvents() {
        return OWNEVENTS.toArray(new ClusterStopEvent[]{});
    }

    @Override
    public ClusterStopEvent[] getInitEvents() {
        return new ClusterStopEvent[]{
                CLUSTER_STOP_EVENT
        };
    }
}

