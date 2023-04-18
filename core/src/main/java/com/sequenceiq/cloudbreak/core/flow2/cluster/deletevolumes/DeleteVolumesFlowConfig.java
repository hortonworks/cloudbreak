package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DELETE_EBS_VOLUMES_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DELETE_EBS_VOLUMES_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DELETE_EBS_VOLUMES_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesState.DELETE_VOLUMES_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesState.DELETE_VOLUMES_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesState.DELETE_VOLUMES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesState.DELETE_VOLUMES_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;

@Component
public class DeleteVolumesFlowConfig  extends StackStatusFinalizerAbstractFlowConfig<DeleteVolumesState, DeleteVolumesEvent>
        implements ClusterUseCaseAware {

    private static final List<Transition<DeleteVolumesState, DeleteVolumesEvent>> TRANSITIONS =
            new Transition.Builder<DeleteVolumesState, DeleteVolumesEvent>()

                    .from(INIT_STATE)
                    .to(DELETE_VOLUMES_VALIDATION_STATE)
                    .event(DELETE_VOLUMES_VALIDATION_EVENT)
                    .failureEvent(FAIL_HANDLED_EVENT)

                    .from(DELETE_VOLUMES_VALIDATION_STATE)
                    .to(DELETE_VOLUMES_STATE)
                    .event(DELETE_VOLUMES_EVENT)
                    .failureEvent(FAIL_HANDLED_EVENT)

                    .from(DELETE_VOLUMES_STATE)
                    .to(DELETE_VOLUMES_FINISHED_STATE)
                    .event(DELETE_VOLUMES_FINISHED_EVENT)
                    .failureEvent(FAIL_HANDLED_EVENT)

                    .from(DELETE_VOLUMES_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .failureEvent(FAIL_HANDLED_EVENT)

                    .build();

    private static final FlowEdgeConfig<DeleteVolumesState, DeleteVolumesEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            DELETE_VOLUMES_FAILED_STATE,
            FAIL_HANDLED_EVENT);

    public DeleteVolumesFlowConfig() {
        super(DeleteVolumesState.class, DeleteVolumesEvent.class);
    }

    @Override
    protected List<Transition<DeleteVolumesState, DeleteVolumesEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DeleteVolumesState, DeleteVolumesEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DeleteVolumesEvent[] getEvents() {
        return DeleteVolumesEvent.values();
    }

    @Override
    public DeleteVolumesEvent[] getInitEvents() {
        return new DeleteVolumesEvent[] {
                DELETE_VOLUMES_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Delete EBS volumes on the stack";
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return DELETE_EBS_VOLUMES_STARTED;
        } else if (FINAL_STATE.equals(flowState)) {
            return DELETE_EBS_VOLUMES_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return DELETE_EBS_VOLUMES_FAILED;
        } else {
            return UNSET;
        }
    }
}
