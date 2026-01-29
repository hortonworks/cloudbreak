package com.sequenceiq.datalake.flow.repair;

import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.repair.SdxRepairState.INIT_STATE;
import static com.sequenceiq.datalake.flow.repair.SdxRepairState.SDX_REPAIR_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.repair.SdxRepairState.SDX_REPAIR_FAILED_STATE;
import static com.sequenceiq.datalake.flow.repair.SdxRepairState.SDX_REPAIR_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.repair.SdxRepairState.SDX_REPAIR_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.repair.SdxRepairState.SDX_REPAIR_START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxRepairFlowConfig extends AbstractFlowConfiguration<SdxRepairState, SdxRepairEvent>
        implements RetryableDatalakeFlowConfiguration<SdxRepairEvent> {

    private static final List<Transition<SdxRepairState, SdxRepairEvent>> TRANSITIONS = new Transition.Builder<SdxRepairState, SdxRepairEvent>()
            .defaultFailureEvent(SDX_REPAIR_FAILED_EVENT)

            .from(INIT_STATE)
            .to(SDX_REPAIR_START_STATE)
            .event(SDX_REPAIR_EVENT)
            .noFailureEvent()

            .from(SDX_REPAIR_START_STATE)
            .to(SDX_REPAIR_IN_PROGRESS_STATE)
            .event(SDX_REPAIR_IN_PROGRESS_EVENT)
            .failureState(SDX_REPAIR_COULD_NOT_START_STATE)
            .failureEvent(SDX_REPAIR_COULD_NOT_START_EVENT)

            .from(SDX_REPAIR_IN_PROGRESS_STATE)
            .to(SDX_REPAIR_FINISHED_STATE)
            .event(SDX_REPAIR_SUCCESS_EVENT)
            .failureEvent(SDX_REPAIR_FAILED_EVENT)

            .from(SDX_REPAIR_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(SDX_REPAIR_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<SdxRepairState, SdxRepairEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_REPAIR_FAILED_STATE, SDX_REPAIR_FAILED_HANDLED_EVENT);

    public SdxRepairFlowConfig() {
        super(SdxRepairState.class, SdxRepairEvent.class);
    }

    @Override
    public SdxRepairEvent[] getEvents() {
        return SdxRepairEvent.values();
    }

    @Override
    public SdxRepairEvent[] getInitEvents() {
        return new SdxRepairEvent[]{
                SDX_REPAIR_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Repair SDX";
    }

    @Override
    protected List<Transition<SdxRepairState, SdxRepairEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxRepairState, SdxRepairEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxRepairEvent getRetryableEvent() {
        return SDX_REPAIR_FAILED_HANDLED_EVENT;
    }

    @Override
    public List<SdxRepairEvent> getStackRetryEvents() {
        return List.of(SDX_REPAIR_IN_PROGRESS_EVENT);
    }
}
