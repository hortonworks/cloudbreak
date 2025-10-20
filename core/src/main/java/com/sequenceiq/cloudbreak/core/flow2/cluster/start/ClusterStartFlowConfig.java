package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_DB_CERT_ROTATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_DB_CERT_ROTATION_FINISH_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_PILLAR_CONFIG_UPDATE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_PILLAR_CONFIG_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.DNS_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_DB_CERT_ROTATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_STARTING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.UPDATING_DNS_IN_PEM_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterStartFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ClusterStartState, ClusterStartEvent>
        implements RetryableFlowConfiguration<ClusterStartEvent> {

    private static final List<Transition<ClusterStartState, ClusterStartEvent>> TRANSITIONS =
            new Builder<ClusterStartState, ClusterStartEvent>()
                    .defaultFailureEvent(CLUSTER_START_FAILURE_EVENT)

                    .from(INIT_STATE).to(CLUSTER_DB_CERT_ROTATION_STATE)
                    .event(CLUSTER_START_EVENT)
                    .noFailureEvent()

                    .from(CLUSTER_DB_CERT_ROTATION_STATE).to(CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE)
                    .event(CLUSTER_DB_CERT_ROTATION_FINISH_EVENT)
                    .failureEvent(CLUSTER_DB_CERT_ROTATION_FAILED_EVENT)

                    .from(CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE).to(UPDATING_DNS_IN_PEM_STATE)
                    .event(CLUSTER_START_PILLAR_CONFIG_UPDATE_FINISHED_EVENT)
                    .failureEvent(CLUSTER_START_PILLAR_CONFIG_UPDATE_FAILED_EVENT)

                    .from(UPDATING_DNS_IN_PEM_STATE).to(CLUSTER_STARTING_STATE)
                    .event(DNS_UPDATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_STARTING_STATE).to(CLUSTER_START_FINISHED_STATE)
                    .event(CLUSTER_START_FINISHED_EVENT)
                    .failureEvent(CLUSTER_START_FAILURE_EVENT)

                    .from(CLUSTER_START_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ClusterStartState, ClusterStartEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_START_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterStartFlowConfig() {
        super(ClusterStartState.class, ClusterStartEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(ClusterStartFlowTriggerCondition.class);
    }

    @Override
    protected List<Transition<ClusterStartState, ClusterStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ClusterStartState, ClusterStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterStartEvent[] getEvents() {
        return ClusterStartEvent.values();
    }

    @Override
    public ClusterStartEvent[] getInitEvents() {
        return new ClusterStartEvent[]{
                CLUSTER_START_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Start cluster";
    }

    @Override
    public ClusterStartEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }
}
