package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_CLUSTER_MANAGER_DECOIMMISSION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_CLUSTER_MANAGER_DECOMMISSIONED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_GET_RECOVERY_CANDIDATES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_INSTANCES_STOPPED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_INSTANCES_STOP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.STOPSTART_DOWNSCALE_DECOMMISSION_VIA_CM_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.STOPSTART_DOWNSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.STOPSTART_DOWNSCALE_FINALIZE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.STOPSTART_DOWNSCALE_GET_RECOVERY_CANDIDATES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.STOPSTART_DOWNSCALE_HOSTS_DECOMMISSION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.STOPSTART_DOWNSCALE_STOP_INSTANCES_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState.STOPSTART_DOWNSCALE_STOP_INSTANCE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;

@Component
public class StopStartDownscaleFlowConfig extends StackStatusFinalizerAbstractFlowConfig<StopStartDownscaleState, StopStartDownscaleEvent> {

    private static final List<Transition<StopStartDownscaleState, StopStartDownscaleEvent>> TRANSITIONS =
            new Transition.Builder<StopStartDownscaleState, StopStartDownscaleEvent>()
            .defaultFailureEvent(STOPSTART_DOWNSCALE_FAILURE_EVENT)
            .from(StopStartDownscaleState.INIT_STATE)
                .to(STOPSTART_DOWNSCALE_GET_RECOVERY_CANDIDATES_STATE)
                .event(STOPSTART_DOWNSCALE_TRIGGER_EVENT)
                .defaultFailureEvent()
            .from(STOPSTART_DOWNSCALE_GET_RECOVERY_CANDIDATES_STATE)
                .to(STOPSTART_DOWNSCALE_HOSTS_DECOMMISSION_STATE)
                .event(STOPSTART_DOWNSCALE_GET_RECOVERY_CANDIDATES_EVENT)
                .defaultFailureEvent()
            .from(STOPSTART_DOWNSCALE_HOSTS_DECOMMISSION_STATE)
                .to(STOPSTART_DOWNSCALE_STOP_INSTANCE_STATE)
                .event(STOPSTART_DOWNSCALE_CLUSTER_MANAGER_DECOMMISSIONED_EVENT)
                .failureState(STOPSTART_DOWNSCALE_DECOMMISSION_VIA_CM_FAILED_STATE)
                .failureEvent(STOPSTART_DOWNSCALE_CLUSTER_MANAGER_DECOIMMISSION_FAILED_EVENT)
            .from(STOPSTART_DOWNSCALE_STOP_INSTANCE_STATE)
                .to(STOPSTART_DOWNSCALE_FINALIZE_STATE)
                .event(STOPSTART_DOWNSCALE_INSTANCES_STOPPED_EVENT)
                .failureState(STOPSTART_DOWNSCALE_STOP_INSTANCES_FAILED_STATE)
                .failureEvent(STOPSTART_DOWNSCALE_INSTANCES_STOP_FAILED_EVENT)
            .from(STOPSTART_DOWNSCALE_DECOMMISSION_VIA_CM_FAILED_STATE)
                .to(STOPSTART_DOWNSCALE_FAILED_STATE)
                .event(STOPSTART_DOWNSCALE_FAILURE_EVENT)
                .defaultFailureEvent()
            .from(STOPSTART_DOWNSCALE_STOP_INSTANCES_FAILED_STATE)
                .to(STOPSTART_DOWNSCALE_FAILED_STATE)
                .event(STOPSTART_DOWNSCALE_FAILURE_EVENT)
                .defaultFailureEvent()
            .from(STOPSTART_DOWNSCALE_FINALIZE_STATE)
                .to(FINAL_STATE)
                .event(STOPSTART_DOWNSCALE_FINALIZED_EVENT)
                .defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StopStartDownscaleState, StopStartDownscaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STOPSTART_DOWNSCALE_FAILED_STATE, STOPSTART_DOWNSCALE_FAIL_HANDLED_EVENT);

    protected StopStartDownscaleFlowConfig() {
        super(StopStartDownscaleState.class, StopStartDownscaleEvent.class);
    }

    @Override
    protected List<Transition<StopStartDownscaleState, StopStartDownscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StopStartDownscaleState, StopStartDownscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StopStartDownscaleEvent[] getEvents() {
        return StopStartDownscaleEvent.values();
    }

    @Override
    public StopStartDownscaleEvent[] getInitEvents() {
        return new StopStartDownscaleEvent[]{STOPSTART_DOWNSCALE_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "StopStart Downscale";
    }
}
