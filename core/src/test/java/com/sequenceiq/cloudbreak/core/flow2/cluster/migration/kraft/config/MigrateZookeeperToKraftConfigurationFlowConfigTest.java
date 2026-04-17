package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.List;

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

    @Test
    @DisplayName("Transitions should form the expected state graph with correct from/to/event mappings")
    void testTransitionsShouldFormTheExpectedStateGraphWithCorrectEventMappings() throws IllegalAccessException {
        List<? extends Transition<?, ?>> transitions = new MigrateZookeeperToKraftConfigurationFlowConfig().getTransitions();

        List<FlowTransition> expectedTransitions = List.of(
                new FlowTransition(INIT_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE,
                        START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE,
                        START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE,
                        FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE,
                        START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_STATE,
                        START_MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE,
                        FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE,
                        FINAL_STATE,
                        FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT)
        );

        assertEquals(expectedTransitions.size(), transitions.size(),
                "The number of transitions does not match the expected number");

        for (int i = 0; i < transitions.size(); i++) {
            Transition<?, ?> transition = transitions.get(i);
            FlowTransition expected = expectedTransitions.get(i);

            assertEquals(expected.from(), transition.getSource(),   "Unexpected 'from' state at index " + i);
            assertEquals(expected.to(),   transition.getTarget(),   "Unexpected 'to' state at index " + i);
            assertEquals(expected.event(), extractEvent(transition), "Unexpected event at index " + i);
        }
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

    private record FlowTransition(FlowState from, FlowState to, FlowEvent event) {
    }
}