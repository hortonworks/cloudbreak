package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate;

import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.StackInstanceMetadataUpdateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.StackInstanceMetadataUpdateState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.StackInstanceMetadataUpdateState.STACK_IMDUPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.StackInstanceMetadataUpdateState.STACK_IMDUPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.StackInstanceMetadataUpdateState.STACK_IMDUPDATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class StackInstanceMetadataUpdateFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent> {

    private static final List<Transition<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent>> TRANSITIONS =
            new Builder<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent>()
                    .defaultFailureEvent(STACK_IMDUPDATE_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(STACK_IMDUPDATE_STATE)
                    .event(STACK_IMDUPDATE_EVENT)
                    .defaultFailureEvent()

                    .from(STACK_IMDUPDATE_STATE)
                    .to(STACK_IMDUPDATE_FINISHED_STATE)
                    .event(STACK_IMDUPDATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(STACK_IMDUPDATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(STACK_IMDUPDATE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            STACK_IMDUPDATE_FAILED_STATE,
            STACK_IMDUPDATE_FAIL_HANDLED_EVENT);

    public StackInstanceMetadataUpdateFlowConfig() {
        super(StackInstanceMetadataUpdateState.class, StackInstanceMetadataUpdateEvent.class);
    }

    @Override
    protected List<Transition<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackInstanceMetadataUpdateEvent[] getEvents() {
        return StackInstanceMetadataUpdateEvent.values();
    }

    @Override
    public StackInstanceMetadataUpdateEvent[] getInitEvents() {
        return new StackInstanceMetadataUpdateEvent[]{
                STACK_IMDUPDATE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Instance metadata update on FreeIPAs instances";
    }

}
