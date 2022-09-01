package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleState.STACK_VERTICALSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleState.STACK_VERTICALSCALE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleState.STACK_VERTICALSCALE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class CoreVerticalScaleFlowConfig extends StackStatusFinalizerAbstractFlowConfig<CoreVerticalScaleState, CoreVerticalScaleEvent> {
    private static final List<Transition<CoreVerticalScaleState, CoreVerticalScaleEvent>> TRANSITIONS =
            new Builder<CoreVerticalScaleState, CoreVerticalScaleEvent>()
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
                    .event(FINALIZED_EVENT)
                        .failureEvent(FAILURE_EVENT)
                    .build();

    private static final FlowEdgeConfig<CoreVerticalScaleState, CoreVerticalScaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            STACK_VERTICALSCALE_FAILED_STATE,
            FAIL_HANDLED_EVENT);

    public CoreVerticalScaleFlowConfig() {
        super(CoreVerticalScaleState.class, CoreVerticalScaleEvent.class);
    }

    @Override
    protected List<Transition<CoreVerticalScaleState, CoreVerticalScaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<CoreVerticalScaleState, CoreVerticalScaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CoreVerticalScaleEvent[] getEvents() {
        return CoreVerticalScaleEvent.values();
    }

    @Override
    public CoreVerticalScaleEvent[] getInitEvents() {
        return new CoreVerticalScaleEvent[] {
                STACK_VERTICALSCALE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Vertical scaling on the stack";
    }

}
