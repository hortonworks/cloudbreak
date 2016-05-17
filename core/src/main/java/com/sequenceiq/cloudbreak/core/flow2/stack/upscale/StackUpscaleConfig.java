package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.ADD_INSTANCES_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.ADD_INSTANCES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.BOOTSTRAP_NEW_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_CONSUL_METADATA_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_CONSUL_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_METADATA_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_FAILED_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackUpscaleConfig extends AbstractFlowConfiguration<StackUpscaleState, StackUpscaleEvent> {
    private static final List<Transition<StackUpscaleState, StackUpscaleEvent>> TRANSITIONS =
            new Transition.Builder<StackUpscaleState, StackUpscaleEvent>()
                    .from(INIT_STATE).to(ADD_INSTANCES_STATE).event(ADD_INSTANCES_EVENT).noFailureEvent()
                    .from(ADD_INSTANCES_STATE).to(ADD_INSTANCES_FINISHED_STATE).event(ADD_INSTANCES_FINISHED_EVENT)
                                    .failureEvent(ADD_INSTANCES_FAILURE_EVENT)
                    .from(ADD_INSTANCES_FINISHED_STATE).to(EXTEND_METADATA_STATE).event(EXTEND_METADATA_EVENT)
                                    .failureEvent(ADD_INSTANCES_FINISHED_FAILURE_EVENT)
                    .from(EXTEND_METADATA_STATE).to(EXTEND_METADATA_FINISHED_STATE).event(EXTEND_METADATA_FINISHED_EVENT)
                                    .failureEvent(EXTEND_METADATA_FAILURE_EVENT)
                    .from(EXTEND_METADATA_FINISHED_STATE).to(BOOTSTRAP_NEW_NODES_STATE).event(StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT)
                                    .failureEvent(StackUpscaleEvent.EXTEND_METADATA_FINISHED_FAILURE_EVENT)
                    .from(BOOTSTRAP_NEW_NODES_STATE).to(EXTEND_CONSUL_METADATA_STATE).event(StackUpscaleEvent.EXTEND_CONSUL_METADATA_EVENT)
                                    .failureEvent(StackUpscaleEvent.BOOTSTRAP_NEW_NODES_FAILURE_EVENT)
                    .from(EXTEND_CONSUL_METADATA_STATE).to(EXTEND_CONSUL_METADATA_FINISHED_STATE).event(StackUpscaleEvent.EXTEND_CONSUL_METADATA_FINISHED_EVENT)
                                    .failureEvent(StackUpscaleEvent.EXTEND_CONSUL_METADATA_FAILURE_EVENT)
                    .from(EXTEND_CONSUL_METADATA_FINISHED_STATE).to(FINAL_STATE).event(UPSCALE_FINALIZED_EVENT)
                                    .failureEvent(StackUpscaleEvent.EXTEND_CONSUL_METADATA_FINISHED_FAILURE_EVENT)
                    .build();

    private static final FlowEdgeConfig<StackUpscaleState, StackUpscaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPSCALE_FAILED_STATE, UPSCALE_FAIL_HANDLED_EVENT);

    public StackUpscaleConfig() {
        super(StackUpscaleState.class, StackUpscaleEvent.class);
    }

    @Override
    protected List<Transition<StackUpscaleState, StackUpscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackUpscaleState, StackUpscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackUpscaleEvent[] getEvents() {
        return StackUpscaleEvent.values();
    }

    @Override
    public StackUpscaleEvent[] getInitEvents() {
        return new StackUpscaleEvent[] {
                StackUpscaleEvent.ADD_INSTANCES_EVENT
        };
    }
}
