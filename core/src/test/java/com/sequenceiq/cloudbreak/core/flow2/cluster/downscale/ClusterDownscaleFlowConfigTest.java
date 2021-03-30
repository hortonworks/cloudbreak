package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.COLLECT_CANDIDATES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.COLLECT_CANDIDATES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.REMOVE_HOSTS_FROM_ORCHESTRATOR_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.REMOVE_HOSTS_FROM_ORCHESTRATOR_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.CLUSTER_DOWNSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.COLLECT_CANDIDATES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.DECOMISSION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.DECOMMISSION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.REMOVE_HOSTS_FROM_ORCHESTRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.UPDATE_INSTANCE_METADATA_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class ClusterDownscaleFlowConfigTest {

    private List<TransitionKeeper> storedTransitions;

    private ClusterDownscaleFlowConfig underTest;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        underTest = new ClusterDownscaleFlowConfig();
        List<Transition<ClusterDownscaleState, ClusterDownscaleEvent>> transitions = underTest.getTransitions();
        storedTransitions = new ArrayList<>(transitions.size());
        int ordinal = 1;
        for (Transition<ClusterDownscaleState, ClusterDownscaleEvent> transition : underTest.getTransitions()) {
            storedTransitions.add(new TransitionKeeper(ordinal, transition.getSource(), transition.getTarget(),
                    extractEventFromAbstractFlowConfigurationTransition(transition), transition.getFailureEvent(), transition.getFailureState()));
            ordinal++;
        }
    }

    @MethodSource("scenarios")
    @ParameterizedTest(name = "{0}")
    void testTransitions(String testName, int expectedOrdinal, ClusterDownscaleState expectedFromState, ClusterDownscaleState expectedToState,
            ClusterDownscaleEvent expectedInitiatedEvent, ClusterDownscaleEvent expectedFailureEvent, ClusterDownscaleState expectedFailureState) {
        List<TransitionKeeper> selectedOnes = storedTransitions.stream()
                .filter(transition -> transition.getFromState() == expectedFromState)
                .collect(Collectors.toList());
        if (selectedOnes.size() == 1) {
            doAssertions(selectedOnes.get(0), expectedOrdinal, expectedToState, expectedInitiatedEvent, expectedFailureEvent,
                    expectedFailureState);
        } else if (selectedOnes.size() > 1) {
            TransitionKeeper selected = selectedOnes.stream()
                    .filter(transition -> transition.getOrdinal() == expectedOrdinal)
                    .findFirst()
                    .get();
            doAssertions(selected, expectedOrdinal, expectedToState, expectedInitiatedEvent, expectedFailureEvent, expectedFailureState);
        } else {
            throw new IllegalStateException("Unable to find the proper test case - input combination!");
        }
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
                // testName                                                                                                                                                                #   source                                          target                                     event                                     failureEvent                            failureState
                { "Init state should be right before the collect candidates state that should start the decommission event",                                                               1,  INIT_STATE,                                     COLLECT_CANDIDATES_STATE,                  DECOMMISSION_EVENT,                       null,                                   null},
                { "The collect candidates state should be right before the decommission state that should start the collect candidates finished event",                                    2,  COLLECT_CANDIDATES_STATE,                       DECOMMISSION_STATE,                        COLLECT_CANDIDATES_FINISHED_EVENT,        COLLECT_CANDIDATES_FAILED_EVENT,        null},
                { "One way is when decommission state is right before the remove hosts from orchestration state that should start the decommission finished event",                        3,  DECOMMISSION_STATE,                             REMOVE_HOSTS_FROM_ORCHESTRATION_STATE,     DECOMMISSION_FINISHED_EVENT,              DECOMMISSION_FAILED_EVENT,              DECOMISSION_FAILED_STATE},
                { "The other way is when the decommission state is right before update instance metadata state that should start the remove hosts from orchestration finished event",      4,  DECOMMISSION_STATE,                             UPDATE_INSTANCE_METADATA_STATE,            REMOVE_HOSTS_FROM_ORCHESTRATOR_FINISHED,  DECOMMISSION_FAILED_EVENT,              DECOMISSION_FAILED_STATE},
                { "Remove hosts from orchestration state should be right before the update instance metadata state that should start the remove hosts from orchestration finished event",  5,  REMOVE_HOSTS_FROM_ORCHESTRATION_STATE,          UPDATE_INSTANCE_METADATA_STATE,            REMOVE_HOSTS_FROM_ORCHESTRATOR_FINISHED,  REMOVE_HOSTS_FROM_ORCHESTRATOR_FAILED,  REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_STATE},
                { "Decommission failed state should be right before the cluster downscale failed state that should start the failure event",                                               6,  DECOMISSION_FAILED_STATE,                       CLUSTER_DOWNSCALE_FAILED_STATE,            FAILURE_EVENT,                            FAILURE_EVENT,                          null},
                { "Remove host from orchestration failed state should be right before the cluster downscale failed state that should start the failure event",                             7,  REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_STATE,   CLUSTER_DOWNSCALE_FAILED_STATE,            FAILURE_EVENT,                            FAILURE_EVENT,                          null},
                { "Update instance metadata state should be right before the final state that should start the finalized event",                                                           8,  UPDATE_INSTANCE_METADATA_STATE,                 FINAL_STATE,                               FINALIZED_EVENT,                          FAILURE_EVENT,                          null},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    private void doAssertions(TransitionKeeper transition, int expectedOrdinal, ClusterDownscaleState expectedToState,
            ClusterDownscaleEvent expectedInitiatedEvent, ClusterDownscaleEvent expectedFailureEvent, ClusterDownscaleState expectedFailureState) {
        assertEquals(expectedOrdinal, transition.getOrdinal(), "The given flow step is not in the expected position in the whole process!");
        assertEquals(expectedInitiatedEvent, transition.getInitiatedEvent(), "The actual initiated event is not matches to the expected!");
        assertEquals(expectedFailureEvent, transition.getFailureEvent(), "The actual failure event is not matches to the expected!");
        assertEquals(expectedFailureState, transition.getFailureState(), "The actual failure state is not matches to the expected!");
        assertEquals(expectedToState, transition.getToState(), "The target state is not matching with the expected!");
    }

    private ClusterDownscaleEvent extractEventFromAbstractFlowConfigurationTransition(Transition<ClusterDownscaleState, ClusterDownscaleEvent> transition)
            throws IllegalAccessException {
        Field field = ReflectionUtils.findField(Transition.class, "event");
        field.setAccessible(true);
        return (ClusterDownscaleEvent) field.get(transition);
    }

    private static final class TransitionKeeper {

        private final int ordinal;

        private final ClusterDownscaleState fromState;

        private final ClusterDownscaleState toState;

        private final ClusterDownscaleEvent initiatedEvent;

        private final ClusterDownscaleEvent failureEvent;

        private final ClusterDownscaleState failureState;

        TransitionKeeper(int ordinal, ClusterDownscaleState fromState, ClusterDownscaleState toState, ClusterDownscaleEvent initiatedEvent,
                ClusterDownscaleEvent failureEvent, ClusterDownscaleState failureState) {
            this.initiatedEvent = initiatedEvent;
            this.failureState = failureState;
            this.failureEvent = failureEvent;
            this.fromState = fromState;
            this.ordinal = ordinal;
            this.toState = toState;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public ClusterDownscaleState getToState() {
            return toState;
        }

        public ClusterDownscaleState getFromState() {
            return fromState;
        }

        public ClusterDownscaleEvent getInitiatedEvent() {
            return initiatedEvent;
        }

        public ClusterDownscaleEvent getFailureEvent() {
            return failureEvent;
        }

        public ClusterDownscaleState getFailureState() {
            return failureState;
        }

    }

}