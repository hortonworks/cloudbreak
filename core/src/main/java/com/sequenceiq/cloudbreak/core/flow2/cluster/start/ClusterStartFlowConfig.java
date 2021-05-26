package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_PILLAR_CONFIG_UPDATE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_PILLAR_CONFIG_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_POLLING_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_POLLING_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_POLLING_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.DNS_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CONFIGURE_MANAGEMENT_SERVICES_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_STARTING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_POLLING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.UPDATING_DNS_IN_PEM_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CONFIGURE_MANAGEMENT_SERVICES_ON_START_STATE;


import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterStartFlowConfig extends AbstractFlowConfiguration<ClusterStartState, ClusterStartEvent>
        implements RetryableFlowConfiguration<ClusterStartEvent> {

    private static final List<Transition<ClusterStartState, ClusterStartEvent>> TRANSITIONS =
            new Builder<ClusterStartState, ClusterStartEvent>()
                    .from(INIT_STATE).to(CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE)
                    .event(CLUSTER_START_EVENT)
                    .noFailureEvent()

                    .from(CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE).to(UPDATING_DNS_IN_PEM_STATE)
                    .event(CLUSTER_START_PILLAR_CONFIG_UPDATE_FINISHED_EVENT)
                    .failureEvent(CLUSTER_START_PILLAR_CONFIG_UPDATE_FAILED_EVENT)

                    .from(UPDATING_DNS_IN_PEM_STATE).to(CLUSTER_STARTING_STATE)
                    .event(DNS_UPDATE_FINISHED_EVENT)
                    .noFailureEvent()

                    .from(CLUSTER_STARTING_STATE).to(CLUSTER_START_POLLING_STATE)
                    .event(CLUSTER_START_POLLING_EVENT)
                    .failureEvent(CLUSTER_START_FAILURE_EVENT)

                    .from(CLUSTER_START_POLLING_STATE).to(CONFIGURE_MANAGEMENT_SERVICES_ON_START_STATE)
                    .event(CLUSTER_START_POLLING_FINISHED_EVENT)
                    .failureEvent(CLUSTER_START_POLLING_FAILURE_EVENT)

                    .from(CONFIGURE_MANAGEMENT_SERVICES_ON_START_STATE)
                    .to(CLUSTER_START_FINISHED_STATE)
                    .event(CONFIGURE_MANAGEMENT_SERVICES_SUCCESS_EVENT)
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
    protected FlowEdgeConfig<ClusterStartState, ClusterStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterStartEvent[] getEvents() {
        return ClusterStartEvent.values();
    }

    @Override
    public ClusterStartEvent[] getInitEvents() {
        return new ClusterStartEvent[] {
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
