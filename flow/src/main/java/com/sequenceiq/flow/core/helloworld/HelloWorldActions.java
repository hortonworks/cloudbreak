package com.sequenceiq.flow.core.helloworld;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.FINALIZE_HELLOWORLD_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_FAILHANDLED_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_SECOND_STEP_FINISHED_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldState;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFailedEvent;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFirstStepLongLastingTaskFailureResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFirstStepLongLastingTaskSuccessResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFirstStepLongLastingTaskTriggerEvent;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFlowTrigger;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldSecondStepSuccessful;

@Configuration
public class HelloWorldActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldActions.class);

    @Bean(name = "HELLO_WORLD_FIRST_STEP_STATE")
    public Action<?, ?> firstStep() {
        return new AbstractHelloWorldAction<>(HelloWorldFlowTrigger.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFlowTrigger payload, Map<Object, Object> variables) {
                LOGGER.info("Hello world first step in progress, we are sending an event for a handler, because it is a long lasting task");
                HelloWorldFirstStepLongLastingTaskTriggerEvent taskTriggerEvent = new HelloWorldFirstStepLongLastingTaskTriggerEvent(payload.getResourceId());
                sendEvent(context, taskTriggerEvent);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FIRST_STEP_FAILED_STATE")
    public Action<?, ?> firstStepFailedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldFirstStepLongLastingTaskFailureResponse.class) {

            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFirstStepLongLastingTaskFailureResponse payload, Map<Object, Object> variables) {
                sendEvent(context, HELLOWORLD_FAILHANDLED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_SECOND_STEP_STATE")
    public Action<?, ?> secondStep() {
        return new AbstractHelloWorldAction<>(HelloWorldFirstStepLongLastingTaskSuccessResponse.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFirstStepLongLastingTaskSuccessResponse payload, Map<Object, Object> variables) {
                LOGGER.info("Hello world second step in progress..");
                HelloWorldSecondStepSuccessful helloWorldSecondStepSuccessful = new HelloWorldSecondStepSuccessful(payload.getResourceId());
                sendEvent(context, HELLOWORLD_SECOND_STEP_FINISHED_EVENT.event(), helloWorldSecondStepSuccessful);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldSecondStepSuccessful.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldSecondStepSuccessful payload, Map<Object, Object> variables) {
                LOGGER.info("Hello world finished!");
                sendEvent(context, FINALIZE_HELLOWORLD_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldFailedEvent.class) {

            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFailedEvent payload, Map<Object, Object> variables) {
                sendEvent(context, HELLOWORLD_FAILHANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractHelloWorldAction<P extends Payload> extends AbstractAction<HelloWorldState, HelloWorldEvent, HelloWorldContext, P> {

        protected AbstractHelloWorldAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected HelloWorldContext createFlowContext(FlowParameters flowParameters, StateContext<HelloWorldState, HelloWorldEvent> stateContext, P payload) {
            return new HelloWorldContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<HelloWorldContext> flowContext, Exception ex) {
            return new HelloWorldFailedEvent(payload.getResourceId(), ex);
        }
    }
}
