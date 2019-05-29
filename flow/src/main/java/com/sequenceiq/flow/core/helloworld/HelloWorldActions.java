package com.sequenceiq.flow.core.helloworld;

import static com.sequenceiq.flow.core.helloworld.HelloWorldEvent.FINALIZE_HELLO_WORLD_EVENT;
import static com.sequenceiq.flow.core.helloworld.HelloWorldEvent.HELLO_WORLD_FAIL_HANDLED_EVENT;
import static com.sequenceiq.flow.core.helloworld.HelloWorldEvent.HELLO_WORLD_FINISHED_EVENT;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class HelloWorldActions {
    @Bean(name = "HELLO_WORLD_START_STATE")
    public Action<?, ?> startAction() {
        return new AbstractHelloWorldAction<>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                sendEvent(context, HELLO_WORLD_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractHelloWorldAction<>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                sendEvent(context, FINALIZE_HELLO_WORLD_EVENT.event(),
                        new HelloWorldFinishPayload(FINALIZE_HELLO_WORLD_EVENT.event(), payload.getResourceId()));
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractHelloWorldAction<>(Payload.class) {

            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                sendEvent(context, HELLO_WORLD_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractHelloWorldAction<P extends Payload> extends AbstractAction<HelloWorldState, HelloWorldEvent, CommonContext, P> {

        protected AbstractHelloWorldAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<HelloWorldState, HelloWorldEvent> stateContext, P payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return (Payload) () -> null;
        }
    }
}
