package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.DEREGISTER_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.DEREGISTER_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.DISABLE_KERBEROS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.DISABLE_KERBEROS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.PREPARE_CLUSTER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.PREPARE_CLUSTER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.PROPER_TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.RECOVERY_TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.CLUSTER_TERMINATING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.CLUSTER_TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.CLUSTER_TERMINATION_FINISH_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.DEREGISTER_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.DISABLE_KERBEROS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.PREPARE_CLUSTER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class ClusterTerminationFlowConfig extends AbstractFlowConfiguration<ClusterTerminationState, ClusterTerminationEvent> {

    private static final List<Transition<ClusterTerminationState, ClusterTerminationEvent>> TRANSITIONS =
            new Builder<ClusterTerminationState, ClusterTerminationEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)
                    .from(INIT_STATE).to(PREPARE_CLUSTER_STATE).event(PROPER_TERMINATION_EVENT)
                        .noFailureEvent()
                    .from(PREPARE_CLUSTER_STATE).to(DEREGISTER_SERVICES_STATE).event(PREPARE_CLUSTER_FINISHED_EVENT)
                        .failureEvent(PREPARE_CLUSTER_FAILED_EVENT)
                    .from(DEREGISTER_SERVICES_STATE).to(DISABLE_KERBEROS_STATE).event(DEREGISTER_SERVICES_FINISHED_EVENT)
                        .failureEvent(DEREGISTER_SERVICES_FAILED_EVENT)
                    .from(DISABLE_KERBEROS_STATE).to(CLUSTER_TERMINATING_STATE).event(DISABLE_KERBEROS_FINISHED_EVENT)
                        .failureEvent(DISABLE_KERBEROS_FAILED_EVENT)

                    .from(INIT_STATE).to(CLUSTER_TERMINATING_STATE).event(TERMINATION_EVENT)
                        .noFailureEvent()
                    .from(CLUSTER_TERMINATING_STATE).to(CLUSTER_TERMINATION_FINISH_STATE).event(TERMINATION_FINISHED_EVENT)
                        .failureEvent(TERMINATION_FAILED_EVENT)
                    .from(CLUSTER_TERMINATION_FINISH_STATE).to(FINAL_STATE).event(FINALIZED_EVENT)
                        .defaultFailureEvent()

                    .from(INIT_STATE).to(CLUSTER_TERMINATING_STATE).event(RECOVERY_TERMINATION_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ClusterTerminationState, ClusterTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CLUSTER_TERMINATION_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterTerminationFlowConfig() {
        super(ClusterTerminationState.class, ClusterTerminationEvent.class);
    }

    @Override
    protected List<Transition<ClusterTerminationState, ClusterTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterTerminationState, ClusterTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterTerminationEvent[] getEvents() {
        return ClusterTerminationEvent.values();
    }

    @Override
    public ClusterTerminationEvent[] getInitEvents() {
        return new ClusterTerminationEvent[]{ TERMINATION_EVENT, PROPER_TERMINATION_EVENT, RECOVERY_TERMINATION_EVENT };
    }

    @Override
    public String getDisplayName() {
        return "Terminate cluster";
    }

}
