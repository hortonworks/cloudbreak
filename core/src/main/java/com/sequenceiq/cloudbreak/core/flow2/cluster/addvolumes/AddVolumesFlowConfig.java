package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_ORCHESTRATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ATTACH_VOLUMES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.ADD_VOLUMES_CM_CONFIGURATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.ADD_VOLUMES_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.ADD_VOLUMES_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.ADD_VOLUMES_ORCHESTRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.ADD_VOLUMES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.ADD_VOLUMES_VALIDATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.ATTACH_VOLUMES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class AddVolumesFlowConfig extends StackStatusFinalizerAbstractFlowConfig<AddVolumesState, AddVolumesEvent>
        implements RetryableFlowConfiguration<AddVolumesEvent> {

    private static final List<Transition<AddVolumesState, AddVolumesEvent>> TRANSITIONS =
            new Builder<AddVolumesState, AddVolumesEvent>()

                    .from(INIT_STATE)
                    .to(ADD_VOLUMES_VALIDATE_STATE)
                    .event(ADD_VOLUMES_TRIGGER_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(ADD_VOLUMES_VALIDATE_STATE)
                    .to(ADD_VOLUMES_STATE)
                    .event(ADD_VOLUMES_VALIDATION_FINISHED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(ADD_VOLUMES_STATE)
                    .to(ATTACH_VOLUMES_STATE)
                    .event(ADD_VOLUMES_FINISHED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(ATTACH_VOLUMES_STATE)
                    .to(ADD_VOLUMES_ORCHESTRATION_STATE)
                    .event(ATTACH_VOLUMES_FINISHED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(ADD_VOLUMES_ORCHESTRATION_STATE)
                    .to(ADD_VOLUMES_CM_CONFIGURATION_STATE)
                    .event(ADD_VOLUMES_ORCHESTRATION_FINISHED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(ADD_VOLUMES_CM_CONFIGURATION_STATE)
                    .to(ADD_VOLUMES_FINISHED_STATE)
                    .event(ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(ADD_VOLUMES_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<AddVolumesState, AddVolumesEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            ADD_VOLUMES_FAILED_STATE,
            ADD_VOLUMES_FAILURE_HANDLED_EVENT);

    public AddVolumesFlowConfig() {
        super(AddVolumesState.class, AddVolumesEvent.class);
    }

    @Override
    protected List<Transition<AddVolumesState, AddVolumesEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<AddVolumesState, AddVolumesEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public AddVolumesEvent[] getEvents() {
        return AddVolumesEvent.values();
    }

    @Override
    public AddVolumesEvent[] getInitEvents() {
        return new AddVolumesEvent[]{
                ADD_VOLUMES_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Resizing disks on the stack";
    }

    @Override
    public AddVolumesEvent getRetryableEvent() {
        return ADD_VOLUMES_FAILURE_HANDLED_EVENT;
    }
}