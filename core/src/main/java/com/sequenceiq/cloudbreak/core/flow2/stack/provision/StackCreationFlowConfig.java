package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGESETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGE_CHECK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.PROVISIONING_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.SETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.STACK_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.START_PROVISIONING_STATE;

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
public final class StackCreationFlowConfig extends AbstractFlowConfiguration<StackCreationState, StackCreationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationFlowConfig.class);
    private static final List<Transition<StackCreationState, StackCreationEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, SETUP_STATE, START_CREATION_EVENT),
            new Transition<>(SETUP_STATE, IMAGESETUP_STATE, SETUP_FINISHED_EVENT),
            new Transition<>(IMAGESETUP_STATE, IMAGE_CHECK_STATE, IMAGE_PREPARATION_FINISHED_EVENT),
            new Transition<>(IMAGE_CHECK_STATE, IMAGE_CHECK_STATE, IMAGE_COPY_CHECK_EVENT),
            new Transition<>(IMAGE_CHECK_STATE, START_PROVISIONING_STATE, IMAGE_COPY_FINISHED_EVENT),
            new Transition<>(START_PROVISIONING_STATE, PROVISIONING_FINISHED_STATE, LAUNCH_STACK_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<StackCreationState, StackCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, PROVISIONING_FINISHED_STATE, STACK_CREATION_FINISHED_EVENT, STACK_CREATION_FAILED_STATE,
                    STACK_CREATION_FAILED_EVENT);

    @Override
    public Flow<StackCreationState, StackCreationEvent> createFlow(String flowId) {
        Flow<StackCreationState, StackCreationEvent> flow = new Flow<>(getStateMachineFactory().getStateMachine(),
                new MessageFactory<StackCreationEvent>(), new StackCreationEventConverter());
        flow.initialize(flowId);
        return flow;
    }

    @Override
    protected MachineConfiguration<StackCreationState, StackCreationEvent> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<StackCreationState, StackCreationEvent> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<StackCreationState, StackCreationEvent> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<StackCreationState, StackCreationEvent> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<StackCreationState, StackCreationEvent> listener =
                new StateMachineListenerAdapter<StackCreationState, StackCreationEvent>() {
                    @Override
                    public void stateChanged(State<StackCreationState, StackCreationEvent> from, State<StackCreationState, StackCreationEvent> to) {
                        LOGGER.info("StackCreationFlow changed from {} to {}", from, to);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    @Override
    protected List<Transition<StackCreationState, StackCreationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackCreationState, StackCreationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public List<StackCreationEvent> getFlowTriggerEvents() {
        return Collections.singletonList(START_CREATION_EVENT);
    }

    @Override
    public StackCreationEvent[] getEvents() {
        return StackCreationEvent.values();
    }
}
