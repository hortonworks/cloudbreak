package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Dependency;

public class DependencyTest {

    private static final String ITEM = "item";

    @Test
    public void testItem() throws Exception {
        Dependency dependency = new Dependency();
        dependency.setItem(ITEM);
        assertEquals(ITEM, dependency.getItem());
    }
}