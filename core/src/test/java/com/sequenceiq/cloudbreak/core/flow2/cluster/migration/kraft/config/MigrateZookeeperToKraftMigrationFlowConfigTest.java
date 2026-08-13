package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.RESTART_KAFKA_BROKER_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.RESTART_KAFKA_CONNECT_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.RESTART_KAFKA_KRAFT_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_BROKER_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_CONNECT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_KRAFT_NODES_EVENT;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class MigrateZookeeperToKraftMigrationFlowConfigTest {

    @Test
    @DisplayName("Transitions should form the expected state graph with correct from/to/event mappings")
    void testTransitionsShouldFormTheExpectedStateGraphWithCorrectEventMappings() throws IllegalAccessException {
        List<? extends Transition<?, ?>> transitions = new MigrateZookeeperToKraftMigrationFlowConfig().getTransitions();

        List<FlowTransition> expectedTransitions = List.of(
                new FlowTransition(INIT_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE,
                        START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE,
                        RESTART_KAFKA_KRAFT_NODES_STATE,
                        START_RESTART_KAFKA_KRAFT_NODES_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_STATE,
                        START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE,
                        FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT),
                new FlowTransition(RESTART_KAFKA_KRAFT_NODES_STATE,
                        RESTART_KAFKA_BROKER_NODES_STATE,
                        START_RESTART_KAFKA_BROKER_NODES_EVENT),
                new FlowTransition(RESTART_KAFKA_BROKER_NODES_STATE,
                        RESTART_KAFKA_CONNECT_NODES_STATE,
                        START_RESTART_KAFKA_CONNECT_NODES_EVENT),
                new FlowTransition(RESTART_KAFKA_CONNECT_NODES_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_STATE,
                        START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_STATE,
                        MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE,
                        FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT),
                new FlowTransition(MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE,
                        FINAL_STATE,
                        FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)
        );

        assertEquals(expectedTransitions.size(), transitions.size(),
                "The number of transitions does not match the expected number");

        for (int i = 0; i < transitions.size(); i++) {
            Transition<?, ?> transition = transitions.get(i);
            FlowTransition expected = expectedTransitions.get(i);

            assertEquals(expected.from(), transition.getSource(), "Unexpected 'from' state at index " + i);
            assertEquals(expected.to(), transition.getTarget(), "Unexpected 'to' state at index " + i);
            assertEquals(expected.event(), extractEvent(transition), "Unexpected event at index " + i);
        }
    }

    @Test
    @DisplayName("Transitions should use default failure event for each step")
    void testTransitionsShouldUseDefaultFailureEventForEachStep() {
        assertDefaultFailureEventForAllTransitions(
                new MigrateZookeeperToKraftMigrationFlowConfig().getTransitions()
        );
    }

    @Test
    @DisplayName("Edge config should define specific init, final and default failure states and failure handled event")
    void testEdgeConfigContainsExpectedStatesAndEvent() {
        AbstractFlowConfiguration.FlowEdgeConfig<MigrateZookeeperToKraftMigrationState, MigrateZookeeperToKraftMigrationStateSelectors>
                configurationEdge = new MigrateZookeeperToKraftMigrationFlowConfig().getEdgeConfig();

        assertEquals(INIT_STATE, configurationEdge.getInitState());
        assertEquals(FINAL_STATE, configurationEdge.getFinalState());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE, configurationEdge.getDefaultFailureState());
        assertEquals(HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT, configurationEdge.getFailureHandled());
    }

    private static void assertDefaultFailureEventForAllTransitions(List<? extends Transition<?, ?>> transitions) {
        for (int i = 0; i < transitions.size(); i++) {
            Transition<?, ?> transition = transitions.get(i);
            assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT, transition.getFailureEvent(),
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
