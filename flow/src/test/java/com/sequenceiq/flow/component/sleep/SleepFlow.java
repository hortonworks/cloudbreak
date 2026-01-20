package com.sequenceiq.flow.component.sleep;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.flow.component.sleep.event.SleepCompletedEvent;
import com.sequenceiq.flow.component.sleep.event.SleepEvent;
import com.sequenceiq.flow.component.sleep.event.SleepFailedEvent;
import com.sequenceiq.flow.component.sleep.event.SleepStartEvent;
import com.sequenceiq.flow.component.sleep.event.SleepWaitRequest;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SleepFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepFlow.class);

    @Bean("SLEEP_STARTED_STATE")
    public Action<?, ?> sleepStarted() {
        return new AbstractAction<>(SleepStartEvent.class) {
            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SleepStartEvent payload) {
                return new CommonContext(flowParameters);
            }

            @Override
            protected void doExecute(CommonContext context, SleepStartEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new SleepWaitRequest(payload.getResourceId(), payload.getSleepDuration(), payload.getFailUntil()));
            }

            @Override
            protected Object getFailurePayload(SleepStartEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return new SleepFailedEvent(payload.getResourceId(), ex.getMessage());
            }
        };
    }

    @Bean("SLEEP_FINISHED_STATE")
    public Action<?, ?> sleepFinished() {
        return new AbstractAction<>(SleepCompletedEvent.class) {
            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SleepCompletedEvent payload) {
                return new CommonContext(flowParameters);
            }

            @Override
            protected void doExecute(CommonContext context, SleepCompletedEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Sleep finalized!");
                sendEvent(context, SleepEvent.SLEEP_FINALIZED_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(SleepCompletedEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return new SleepFailedEvent(payload.getResourceId(), ex.getMessage());
            }
        };
    }

    @Bean("SLEEP_FAILED_STATE")
    public Action<?, ?> sleepFailed() {
        return new AbstractAction<>(SleepFailedEvent.class) {
            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SleepFailedEvent payload) {
                return new CommonContext(flowParameters);
            }

            @Override
            protected void doExecute(CommonContext context, SleepFailedEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Sleep fail handled!");
                sendEvent(context, SleepEvent.SLEEP_FAIL_HANDLED_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(SleepFailedEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean
    public SleepFlowConfig sleepFlowConfig() {
        return new SleepFlowConfig();
    }

    @Bean
    public SleepWaitHandler sleepWaitHandler() {
        return new SleepWaitHandler();
    }

    @Bean
    public SleepChainEventFactory sleepChainEventFactory() {
        return new SleepChainEventFactory();
    }

    @Bean
    public NestedSleepChainEventFactory nestedSleepChainEventFactory() {
        return new NestedSleepChainEventFactory();
    }
}
