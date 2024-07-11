package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_REMOVE_USERDATA_SECRETS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_REMOVE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_RESOURCES_COLLECTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_RESOURCES_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_COLLECT_RESOURCES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class StackDownscaleConfig extends StackStatusFinalizerAbstractFlowConfig<StackDownscaleState, StackDownscaleEvent> {
    private static final List<Transition<StackDownscaleState, StackDownscaleEvent>> TRANSITIONS =
            new Builder<StackDownscaleState, StackDownscaleEvent>()
                    .from(INIT_STATE).to(DOWNSCALE_COLLECT_RESOURCES_STATE).event(STACK_DOWNSCALE_EVENT).noFailureEvent()
                    .from(DOWNSCALE_COLLECT_RESOURCES_STATE).to(DOWNSCALE_STATE).event(DOWNSCALE_RESOURCES_COLLECTED_EVENT)
                    .failureEvent(DOWNSCALE_RESOURCES_FAILURE_EVENT)
                    .from(DOWNSCALE_STATE).to(DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE).event(DOWNSCALE_FINISHED_EVENT)
                    .failureEvent(DOWNSCALE_FAILURE_EVENT)
                    .from(DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE).to(DOWNSCALE_FINISHED_STATE).event(DOWNSCALE_REMOVE_USERDATA_SECRETS_FINISHED_EVENT)
                    .failureEvent(DOWNSCALE_REMOVE_USERDATA_SECRETS_FAILED_EVENT)
                    .from(DOWNSCALE_FINISHED_STATE).to(FINAL_STATE).event(DOWNSCALE_FINALIZED_EVENT).noFailureEvent()
                    .build();

    private static final FlowEdgeConfig<StackDownscaleState, StackDownscaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DOWNSCALE_FAILED_STATE, DOWNSCALE_FAIL_HANDLED_EVENT);

    public StackDownscaleConfig() {
        super(StackDownscaleState.class, StackDownscaleEvent.class);
    }

    @Override
    public StackDownscaleEvent[] getEvents() {
        return StackDownscaleEvent.values();
    }

    @Override
    public StackDownscaleEvent[] getInitEvents() {
        return new StackDownscaleEvent[]{
                STACK_DOWNSCALE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Stack downscale";
    }

    @Override
    protected List<Transition<StackDownscaleState, StackDownscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StackDownscaleState, StackDownscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
