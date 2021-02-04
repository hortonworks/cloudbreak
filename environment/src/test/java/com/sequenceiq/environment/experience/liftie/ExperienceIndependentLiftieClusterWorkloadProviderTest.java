package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ExperienceIndependentLiftieClusterWorkloadProviderTest {

    @Test
    void testWhenInputWorkloadLabelSetIsNullThenEmptySetShouldReturn() {
        Set<String> result = new ExperienceIndependentLiftieClusterWorkloadProvider(null).getWorkloadsLabels();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testWhenInputWorkloadLabelSetIsEmptyThenEmptySetShouldReturn() {
        Set<String> empty = new LinkedHashSet<>(0);
        Set<String> result = new ExperienceIndependentLiftieClusterWorkloadProvider(empty).getWorkloadsLabels();

        assertNotNull(result);
        assertEquals(empty, result);
    }

    @Test
    void testWhenInputWorkloadLabelSetIsNotEmptyThenThatSetShouldReturn() {
        Set<String> workloadLabels = Set.of("label1", "label2");
        Set<String> result = new ExperienceIndependentLiftieClusterWorkloadProvider(workloadLabels).getWorkloadsLabels();

        assertNotNull(result);
        assertEquals(workloadLabels, result);
    }

}