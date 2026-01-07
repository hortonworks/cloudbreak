package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class MigrateZookeeperToKraftConfigurationFlowConfigTest {

    private static final List<MigrateZookeeperToKraftConfigurationState> EXPECTED_STATE_CHAIN = List.of(
            INIT_STATE,
            MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE,
            MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE,
            MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE,
            MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE,
            FINAL_STATE
    );

    private static final List<MigrateZookeeperToKraftConfigurationStateSelectors> EXPECTED_EVENT_CHAIN = List.of(
            START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT,
            START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT,
            START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT,
            FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT,
            FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT
    );

    @Test
    @DisplayName("Transitions should form a linear and constant chain with exactly specified from states and expected events")
    void testTransitionsShouldFormALinearChainWithUniqueFromStatesAndExpectedEvents() throws IllegalAccessException {
        checkChainAndFromStates(new MigrateZookeeperToKraftConfigurationFlowConfig().getTransitions());
    }

    @Test
    @DisplayName("Transitions should use default failure event for each step")
    void testTransitionsShouldUseDefaultFailureEventForEachStep() {
        assertDefaultFailureEventForAllTransitions(
                new MigrateZookeeperToKraftConfigurationFlowConfig().getTransitions()
        );
    }

    @Test
    @DisplayName("Edge config should define specific init, final and default failure states and failure handled event")
    void testEdgeConfigContainsExpectedStatesAndEvent() {
        AbstractFlowConfiguration.FlowEdgeConfig<MigrateZookeeperToKraftConfigurationState, MigrateZookeeperToKraftConfigurationStateSelectors>
                configurationEdge = new MigrateZookeeperToKraftConfigurationFlowConfig().getEdgeConfig();

        assertEquals(INIT_STATE, configurationEdge.getInitState());
        assertEquals(FINAL_STATE, configurationEdge.getFinalState());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE, configurationEdge.getDefaultFailureState());
        assertEquals(HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT, configurationEdge.getFailureHandled());
    }

    private static void checkChainAndFromStates(List<? extends Transition<?, ?>> transitions) throws IllegalAccessException {
        assertEquals(MigrateZookeeperToKraftConfigurationFlowConfigTest.EXPECTED_STATE_CHAIN.size() - 1, transitions.size(),
                "The number of transitions does not match with the expected number of steps");

        Map<FlowState, Integer> fromCounts = new HashMap<>();
        for (int i = 0; i < transitions.size(); i++) {
            Transition<?, ?> transition = transitions.get(i);

            FlowState expectedFrom = ((List<? extends FlowState>) MigrateZookeeperToKraftConfigurationFlowConfigTest.EXPECTED_STATE_CHAIN).get(i);
            FlowState expectedTo = ((List<? extends FlowState>) MigrateZookeeperToKraftConfigurationFlowConfigTest.EXPECTED_STATE_CHAIN).get(i + 1);
            FlowEvent expectedEvent = ((List<? extends FlowEvent>) MigrateZookeeperToKraftConfigurationFlowConfigTest.EXPECTED_EVENT_CHAIN).get(i);

            assertEquals(expectedFrom, transition.getSource(), "Unexpected 'from' state at transition index " + i);
            assertEquals(expectedTo, transition.getTarget(), "Unexpected 'to' state at transition index " + i);
            assertEquals(expectedEvent, extractEvent(transition), "Unexpected event at transition index " + i);

            fromCounts.merge(transition.getSource(), 1, Integer::sum);
        }

        for (Map.Entry<FlowState, Integer> entry : fromCounts.entrySet()) {
            assertEquals(1, entry.getValue(), "State appears more than once as 'from': " + entry.getKey());
        }
    }

    private static void assertDefaultFailureEventForAllTransitions(List<? extends Transition<?, ?>> transitions) {
        for (int i = 0; i < transitions.size(); i++) {
            Transition<?, ?> transition = transitions.get(i);
            assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT, transition.getFailureEvent(),
                    "Unexpected failure event at transition index " + i);
            assertNull(transition.getFailureState(), "Failure state should be null for default failure event at transition index " + i);
        }
    }

    private static FlowEvent extractEvent(Transition<?, ?> transition) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(Transition.class, "event");
        ReflectionUtils.makeAccessible(requireNonNull(field));
        return (FlowEvent) field.get(transition);
    }

}