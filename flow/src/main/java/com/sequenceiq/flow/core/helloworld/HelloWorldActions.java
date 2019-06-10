package com.sequenceiq.flow.core.helloworld;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.FINALIZE_HELLOWORLD_EVENT;
import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_FAILHANDLED_EVENT;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldState;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFlowTriggerEvent;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldLongLastingTaskFailureResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldLongLastingTaskSuccessResponse;
import com.sequenceiq.flow.core.helloworld.reactor.HelloWorldLongLastingTaskTriggerEvent;

@Configuration
public class HelloWorldActions {
    @Bean(name = "HELLO_WORLD_START_STATE")
    public Action<?, ?> startAction() {
        return new AbstractHelloWorldAction<>(HelloWorldFlowTriggerEvent.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFlowTriggerEvent payload, Map<Object, Object> variables) {
                HelloWorldLongLastingTaskTriggerEvent taskTriggerEvent = new HelloWorldLongLastingTaskTriggerEvent(payload.getResourceId());
                sendEvent(context, taskTriggerEvent);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldLongLastingTaskSuccessResponse.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldLongLastingTaskSuccessResponse payload, Map<Object, Object> variables) {
                sendEvent(context, FINALIZE_HELLOWORLD_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldLongLastingTaskFailureResponse.class) {

            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldLongLastingTaskFailureResponse payload, Map<Object, Object> variables) {
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
            return (Payload) () -> null;
        }
    }
}
