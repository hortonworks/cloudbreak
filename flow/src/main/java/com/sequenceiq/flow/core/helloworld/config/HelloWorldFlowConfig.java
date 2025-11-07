package com.sequenceiq.flow.core.helloworld.config;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.FINALIZE_HELLOWORLD_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_FAILHANDLED_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_FIRST_STEP_FINISHED_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_FIRST_STEP_WENT_WRONG_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_SECOND_STEP_FINISHED_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_SOMETHING_WENT_WRONG;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldState.FINAL_STATE;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldState.HELLO_WORLD_FAILED_STATE;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldState.HELLO_WORLD_FINISHED_STATE;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldState.HELLO_WORLD_FIRST_STEP_STATE;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldState.HELLO_WORLD_SECOND_STEP_STATE;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class HelloWorldFlowConfig extends AbstractFlowConfiguration<HelloWorldState, HelloWorldEvent> implements RetryableFlowConfiguration<HelloWorldEvent> {
    private static final List<Transition<HelloWorldState, HelloWorldEvent>> TRANSITIONS = new Transition.Builder<HelloWorldState, HelloWorldEvent>()
            .defaultFailureEvent(HELLOWORLD_SOMETHING_WENT_WRONG)

            .from(INIT_STATE)
            .to(HELLO_WORLD_FIRST_STEP_STATE)
            .event(HELLOWORLD_TRIGGER_EVENT)
            .noFailureEvent()

            .from(HELLO_WORLD_FIRST_STEP_STATE)
            .to(HELLO_WORLD_SECOND_STEP_STATE)
            .event(HELLOWORLD_FIRST_STEP_FINISHED_EVENT)
            .failureEvent(HELLOWORLD_FIRST_STEP_WENT_WRONG_EVENT)

            .from(HELLO_WORLD_SECOND_STEP_STATE)
            .to(HELLO_WORLD_FINISHED_STATE)
            .event(HELLOWORLD_SECOND_STEP_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(HELLO_WORLD_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_HELLOWORLD_EVENT)
            .defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<HelloWorldState, HelloWorldEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, HELLO_WORLD_FAILED_STATE, HELLOWORLD_FAILHANDLED_EVENT);

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
                HELLOWORLD_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Hello world";
    }

    @Override
    public HelloWorldEvent getRetryableEvent() {
        return HELLOWORLD_FAILHANDLED_EVENT;
    }

    @Override
    protected List<Transition<HelloWorldState, HelloWorldEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<HelloWorldState, HelloWorldEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
