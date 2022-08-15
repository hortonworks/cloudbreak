package com.sequenceiq.freeipa.flow.freeipa.verticalscale;

import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleState.STACK_VERTICALSCALE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleState.STACK_VERTICALSCALE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleState.STACK_VERTICALSCALE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent;

@Component
public class FreeIPAVerticalScaleFlowConfig extends AbstractFlowConfiguration<FreeIPAVerticalScaleState, FreeIPAVerticalScaleEvent> {
    private static final List<Transition<FreeIPAVerticalScaleState, FreeIPAVerticalScaleEvent>> TRANSITIONS =
            new Builder<FreeIPAVerticalScaleState, FreeIPAVerticalScaleEvent>()
                    .from(INIT_STATE)
                    .to(STACK_VERTICALSCALE_STATE)
                    .event(STACK_VERTICALSCALE_EVENT)
                    .noFailureEvent()

                    .from(STACK_VERTICALSCALE_STATE)
                    .to(STACK_VERTICALSCALE_FINISHED_STATE)
                    .event(STACK_VERTICALSCALE_FINISHED_EVENT)
                    .failureEvent(STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT)

                    .from(STACK_VERTICALSCALE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(STACK_VERTICALSCALE_FINALIZED_EVENT)
                    .failureEvent(STACK_VERTICALSCALE_FAILURE_EVENT)

                    .build();

    private static final FlowEdgeConfig<FreeIPAVerticalScaleState, FreeIPAVerticalScaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            STACK_VERTICALSCALE_FAILED_STATE,
            STACK_VERTICALSCALE_FAIL_HANDLED_EVENT);

    public FreeIPAVerticalScaleFlowConfig() {
        super(FreeIPAVerticalScaleState.class, FreeIPAVerticalScaleEvent.class);
    }

    @Override
    protected List<Transition<FreeIPAVerticalScaleState, FreeIPAVerticalScaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<FreeIPAVerticalScaleState, FreeIPAVerticalScaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIPAVerticalScaleEvent[] getEvents() {
        return FreeIPAVerticalScaleEvent.values();
    }

    @Override
    public FreeIPAVerticalScaleEvent[] getInitEvents() {
        return new FreeIPAVerticalScaleEvent[] {
                STACK_VERTICALSCALE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Vertical scaling on the FreeIPA";
    }

}
