package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Resource;

class ResourceTest {

    private static final int CPUS = 1;

    private static final int MEMORY = 1024;

    @Test
    void testCpus() {
        Resource resource = new Resource();
        resource.setCpus(CPUS);
        assertEquals(CPUS, resource.getCpus());
    }

    @Test
    void testMemory() {
        Resource resource = new Resource();
        resource.setMemory(MEMORY);
        assertEquals(MEMORY, resource.getMemory());
    }
}