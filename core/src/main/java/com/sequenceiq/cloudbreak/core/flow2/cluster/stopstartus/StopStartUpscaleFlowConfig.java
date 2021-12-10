package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_CLUSTER_MANAGER_COMMISSIONED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_INSTANCES_STARTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleState.STOPSTART_UPSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleState.STOPSTART_UPSCALE_FINALIZE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleState.STOPSTART_UPSCALE_HOSTS_COMMISSION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleState.STOPSTART_UPSCALE_START_INSTANCE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class StopStartUpscaleFlowConfig extends AbstractFlowConfiguration<StopStartUpscaleState, StopStartUpscaleEvent>
        implements RetryableFlowConfiguration<StopStartUpscaleEvent> {

    // TODO CB-14929: Additional states, transitions for error handling, reverting operations when possible, etc
    //  Expected sequencing - short term.
    //  1. Find Cloud Provider nodes in STOPPED state (This goes beyond looking up CB state)
    //  2. Start the nodes on the Cloud Provider.
    //  3. Commission + Start + End-Maintenance - on list of nodes which were successfully startted (Don't attempt any auto-repair)

    // TODO CB-14929: Better error handling
    //  Expected sequencing - longer term.
    //  1. Rationalize CB data, CM data and cloud-provider data.
    //      Candidate nodes: [CloudState: RUNNING, CMState: ServicesUnhealthy/Stpoped], [CloudState: STOPPED]
    //  2. Start the Cloud instances which need to be STARTED.
    //  3. Make sure CM commission actiion has adequate information to take specific action
    //   (e.g. StartServices on already COMMISSIONED nodes, EndMaintenance, commission, start services on cloud-provider STARTED nodes)
    //  -4- Error Handling in case of failures along these steps.

    private static final List<Transition<StopStartUpscaleState, StopStartUpscaleEvent>> TRANSITIONS =
            new Transition.Builder<StopStartUpscaleState, StopStartUpscaleEvent>()
            .defaultFailureEvent(STOPSTART_UPSCALE_FAILURE_EVENT)
            .from(INIT_STATE)
                    .to(STOPSTART_UPSCALE_START_INSTANCE_STATE)
                    .event(STOPSTART_UPSCALE_TRIGGER_EVENT)
                    .defaultFailureEvent()
            .from(STOPSTART_UPSCALE_START_INSTANCE_STATE)
                    .to(STOPSTART_UPSCALE_HOSTS_COMMISSION_STATE)
                    .event(STOPSTART_UPSCALE_INSTANCES_STARTED_EVENT)
                    .defaultFailureEvent()
                    // TODO CB-14929. Add a failureState() + failureEvent() transition - for any repair action that may be necessary,
                    //  This likely involves adding a new set of transitions from this failure state to the final failed state
                    //  (i.e. STOPSTART_UPSCALE_FAILED_STATE)
            .from(STOPSTART_UPSCALE_HOSTS_COMMISSION_STATE)
                    .to(STOPSTART_UPSCALE_FINALIZE_STATE)
                    .event(STOPSTART_UPSCALE_CLUSTER_MANAGER_COMMISSIONED_EVENT)
                    .defaultFailureEvent()
                    // TODO CB-14929. Add a failureState() + failureEvent() transition - for any repair action that may be necessary,
                    //  This likely involves adding a new set of transitions from this failure state to the final failed state
                    //  (i.e. STOPSTART_UPSCALE_FAILED_STATE)
            .from(STOPSTART_UPSCALE_FINALIZE_STATE)
                    .to(FINAL_STATE)
                    .event(STOPSTART_UPSCALE_FINALIZED_EVENT)
                    .defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StopStartUpscaleState, StopStartUpscaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STOPSTART_UPSCALE_FAILED_STATE, STOPSTART_UPSCALE_FAIL_HANDLED_EVENT);

    protected StopStartUpscaleFlowConfig() {
        super(StopStartUpscaleState.class, StopStartUpscaleEvent.class);
    }

    @Override
    protected List<Transition<StopStartUpscaleState, StopStartUpscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StopStartUpscaleState, StopStartUpscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StopStartUpscaleEvent[] getEvents() {
        return  StopStartUpscaleEvent.values();
    }

    @Override
    public StopStartUpscaleEvent[] getInitEvents() {
        return new StopStartUpscaleEvent[]{STOPSTART_UPSCALE_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "StopStart Upscale";
    }

    @Override
    public StopStartUpscaleEvent getRetryableEvent() {
        return STOPSTART_UPSCALE_FAIL_HANDLED_EVENT;
    }
}
