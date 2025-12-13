package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Dependency;

class DependencyTest {

    private static final String ITEM = "item";

    @Test
    void testItem() {
        Dependency dependency = new Dependency();
        dependency.setItem(ITEM);
        assertEquals(ITEM, dependency.getItem());
    }
}