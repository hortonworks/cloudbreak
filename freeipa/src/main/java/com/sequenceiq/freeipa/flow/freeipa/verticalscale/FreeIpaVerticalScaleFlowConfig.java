package com.sequenceiq.freeipa.flow.freeipa.verticalscale;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.VERTICAL_SCALE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.VERTICAL_SCALE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.VERTICAL_SCALE_STARTED;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleState.STACK_VERTICALSCALE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleState.STACK_VERTICALSCALE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleState.STACK_VERTICALSCALE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent;

@Component
public class FreeIpaVerticalScaleFlowConfig extends StackStatusFinalizerAbstractFlowConfig<FreeIpaVerticalScaleState, FreeIpaVerticalScaleEvent>
        implements FreeIpaUseCaseAware {
    private static final List<Transition<FreeIpaVerticalScaleState, FreeIpaVerticalScaleEvent>> TRANSITIONS =
            new Builder<FreeIpaVerticalScaleState, FreeIpaVerticalScaleEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(STACK_VERTICALSCALE_STATE)
                    .event(STACK_VERTICALSCALE_EVENT)
                    .failureEvent(STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT)

                    .from(STACK_VERTICALSCALE_STATE)
                    .to(STACK_VERTICALSCALE_FINISHED_STATE)
                    .event(STACK_VERTICALSCALE_FINISHED_EVENT)
                    .failureEvent(STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT)

                    .from(STACK_VERTICALSCALE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .failureEvent(STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT)

                    .build();

    private static final FlowEdgeConfig<FreeIpaVerticalScaleState, FreeIpaVerticalScaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            STACK_VERTICALSCALE_FAILED_STATE,
            STACK_VERTICALSCALE_FAIL_HANDLED_EVENT);

    public FreeIpaVerticalScaleFlowConfig() {
        super(FreeIpaVerticalScaleState.class, FreeIpaVerticalScaleEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaVerticalScaleState, FreeIpaVerticalScaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaVerticalScaleState, FreeIpaVerticalScaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaVerticalScaleEvent[] getEvents() {
        return FreeIpaVerticalScaleEvent.values();
    }

    @Override
    public FreeIpaVerticalScaleEvent[] getInitEvents() {
        return new FreeIpaVerticalScaleEvent[] {
                STACK_VERTICALSCALE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Vertical scaling on the FreeIPA";
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return VERTICAL_SCALE_STARTED;
        } else if (STACK_VERTICALSCALE_FINISHED_STATE.equals(flowState)) {
            return VERTICAL_SCALE_FINISHED;
        } else if (STACK_VERTICALSCALE_FAILED_STATE.equals(flowState)) {
            return VERTICAL_SCALE_FAILED;
        } else {
            return UNSET;
        }
    }

}
