package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Resource;

public class ResourceTest {

    private static final int CPUS = 1;

    private static final int MEMORY = 1024;

    @Test
    public void testCpus() throws Exception {
        Resource resource = new Resource();
        resource.setCpus(CPUS);
        assertEquals(CPUS, resource.getCpus());
    }

    @Test
    public void testMemory() throws Exception {
        Resource resource = new Resource();
        resource.setMemory(MEMORY);
        assertEquals(MEMORY, resource.getMemory());
    }
}