package com.sequenceiq.freeipa.flow.stack.migration;

import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.CREATE_RESOURCES_STATE;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.INIT_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class AwsVariantMigrationFlowConfigTest {

    private final AwsVariantMigrationFlowConfig underTest = new AwsVariantMigrationFlowConfig();

    @Test
    void initTransitionsIntoCreateResourcesOnCreateResourcesEvent() {
        List<Transition<AwsVariantMigrationFlowState, AwsVariantMigrationEvent>> transitions = underTest.getTransitions();

        Transition<AwsVariantMigrationFlowState, AwsVariantMigrationEvent> current = transitions.stream()
                .filter(t -> t.getSource() == INIT_STATE && t.getEvent() == CREATE_RESOURCES_EVENT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("INIT_STATE -> CREATE_RESOURCES_STATE transition is missing."));
        assertEquals(CREATE_RESOURCES_STATE, current.getTarget());
    }

    @Test
    void initEventsContainCreateResourcesEvent() {
        List<AwsVariantMigrationEvent> initEvents = Arrays.asList(underTest.getInitEvents());
        assertTrue(initEvents.contains(CREATE_RESOURCES_EVENT), "CREATE_RESOURCES_EVENT must be an init event");
    }
}
