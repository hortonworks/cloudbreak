package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_STATE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackStopFlowConfig extends AbstractFlowConfiguration<StackStopState, StackStopEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopFlowConfig.class);
    private static final List<Transition<StackStopState, StackStopEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, STOP_STATE, STOP_EVENT),
            new Transition<>(STOP_STATE, STOP_FINISHED_STATE, STOP_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<StackStopState, StackStopEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STOP_FINISHED_STATE, STOP_FINALIZED_EVENT, STOP_FAILED_STATE, STOP_FAIL_HANDLED_EVENT);

    @Override
    public Flow<StackStopState, StackStopEvent> createFlow(String flowId) {
        Flow<StackStopState, StackStopEvent> flow = new Flow<>(getStateMachineFactory().getStateMachine(),
                new MessageFactory<StackStopEvent>(), new StackStopEventConverter());
        flow.initialize(flowId);
        return flow;
    }

    @Override
    public List<StackStopEvent> getFlowTriggerEvents() {
        return Collections.singletonList(STOP_EVENT);
    }

    @Override
    public StackStopEvent[] getEvents() {
        return StackStopEvent.values();
    }

    @Override
    protected MachineConfiguration<StackStopState, StackStopEvent> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<StackStopState, StackStopEvent> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<StackStopState, StackStopEvent> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<StackStopState, StackStopEvent> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<StackStopState, StackStopEvent> listener =
                new StateMachineListenerAdapter<StackStopState, StackStopEvent>() {
                    @Override
                    public void stateChanged(State<StackStopState, StackStopEvent> from, State<StackStopState, StackStopEvent> to) {
                        LOGGER.info("StackStopFlowConfig changed from {} to {}", from, to);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    @Override
    protected List<Transition<StackStopState, StackStopEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackStopState, StackStopEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
