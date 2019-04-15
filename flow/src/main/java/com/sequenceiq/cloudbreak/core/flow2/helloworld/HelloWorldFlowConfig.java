package com.sequenceiq.cloudbreak.core.flow2.helloworld;

import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldEvent.FINALIZE_HELLO_WORLD_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldEvent.HELLO_WORLD_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldEvent.HELLO_WORLD_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldEvent.HELLO_WORLD_SOMETHING_WENT_WRONG;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldEvent.START_HELLO_WORLD_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldState.HELLO_WORLD_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldState.HELLO_WORLD_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldState.HELLO_WORLD_START_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.cloudbreak.core.flow2.config.RetryableFlowConfiguration;

@Component
public class HelloWorldFlowConfig extends AbstractFlowConfiguration<HelloWorldState, HelloWorldEvent> implements RetryableFlowConfiguration<HelloWorldEvent> {
    private static final List<Transition<HelloWorldState, HelloWorldEvent>> TRANSITIONS = new Builder<HelloWorldState, HelloWorldEvent>()
            .defaultFailureEvent(HELLO_WORLD_SOMETHING_WENT_WRONG)
            .from(INIT_STATE).to(HELLO_WORLD_START_STATE).event(START_HELLO_WORLD_EVENT).noFailureEvent()
            .from(HELLO_WORLD_START_STATE).to(HELLO_WORLD_FINISHED_STATE).event(HELLO_WORLD_FINISHED_EVENT).defaultFailureEvent()
            .from(HELLO_WORLD_FINISHED_STATE).to(FINAL_STATE).event(FINALIZE_HELLO_WORLD_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<HelloWorldState, HelloWorldEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, HELLO_WORLD_FAILED_STATE, HELLO_WORLD_FAIL_HANDLED_EVENT);

    public HelloWorldFlowConfig() {
        super(HelloWorldState.class, HelloWorldEvent.class);
    }

    @Override
    public HelloWorldEvent[] getEvents() {
        return HelloWorldEvent.values();
    }

    @Override
    public HelloWorldEvent[] getInitEvents() {
        return new HelloWorldEvent[] {
                START_HELLO_WORLD_EVENT
        };
    }

    @Override
    public HelloWorldEvent getFailHandledEvent() {
        return HELLO_WORLD_FAIL_HANDLED_EVENT;
    }

    @Override
    protected List<Transition<HelloWorldState, HelloWorldEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<HelloWorldState, HelloWorldEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
