package com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState.RESTART_CLUSTER_MANAGER_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState.RESTART_CLUSTER_MANAGER_FINISED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState.RESTART_CLUSTER_MANAGER_FLOW_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;

@Component
public class RestartClusterManagerFlowConfig extends StackStatusFinalizerAbstractFlowConfig<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent> {

    public static final FlowEdgeConfig<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, RESTART_CLUSTER_MANAGER_FAILED_STATE,
                    RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FAIL_HANDLED_EVENT);

    private static final List<Transition<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent>> TRANSITIONS =
            new Transition.Builder<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent>()
                    .defaultFailureEvent(RESTART_CLUSTER_MANAGER_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(RESTART_CLUSTER_MANAGER_FLOW_STATE)
                    .event(RESTART_CLUSTER_MANAGER_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(RESTART_CLUSTER_MANAGER_FLOW_STATE)
                    .to(RESTART_CLUSTER_MANAGER_FINISED_STATE)
                    .event(RESTART_CLUSTER_MANAGER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(RESTART_CLUSTER_MANAGER_FINISED_STATE)
                    .to(FINAL_STATE)
                    .event(RESTART_CLUSTER_MANAGER_FINALIZED_EVENT)
                    .defaultFailureEvent()
                    .build();

    protected RestartClusterManagerFlowConfig() {
        super(RestartClusterManagerFlowState.class, RestartClusterManagerFlowEvent.class);
    }

    @Override
    protected List<Transition<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RestartClusterManagerFlowEvent[] getEvents() {
        return RestartClusterManagerFlowEvent.values();
    }

    @Override
    public RestartClusterManagerFlowEvent[] getInitEvents() {
        return new RestartClusterManagerFlowEvent[] {
                RESTART_CLUSTER_MANAGER_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Restart Cluster Manager";
    }

}
