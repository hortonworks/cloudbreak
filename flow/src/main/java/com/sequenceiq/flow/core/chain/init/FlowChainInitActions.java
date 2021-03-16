package com.sequenceiq.flow.core.chain.init;

import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_FAILHANDLED_EVENT;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_FINISHED_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitFailedPayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Configuration
public class FlowChainInitActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainInitActions.class);

    @Bean(name = "FLOWCHAIN_INIT_FINISHED_STATE")
    public Action<?, ?> initAction() {
        return new AbstractFlowChainInitAction<>(FlowChainInitPayload.class) {
            @Override
            protected void doExecute(CommonContext context, FlowChainInitPayload payload, Map<Object, Object> variables) {
                LOGGER.info("Flow chain {} initialized", payload.getFlowChainName());
                sendEvent(context, FLOWCHAIN_INIT_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "FLOWCHAIN_INIT_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractFlowChainInitAction<>(FlowChainInitFailedPayload.class) {
            @Override
            protected void doExecute(CommonContext context, FlowChainInitFailedPayload payload, Map<Object, Object> variables) {
                sendEvent(context, FLOWCHAIN_INIT_FAILHANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractFlowChainInitAction<P extends FlowChainInitPayload>
            extends AbstractAction<FlowChainInitState, FlowChainInitEvent, CommonContext, P> {

        protected AbstractFlowChainInitAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<FlowChainInitState, FlowChainInitEvent> stateContext, P payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return new FlowChainInitFailedPayload(payload.getFlowChainName(), payload.getResourceId(), ex);
        }
    }
}
