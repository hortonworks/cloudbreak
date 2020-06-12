package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GrainTargetTest {

    private static final String TARGET_1 = "ipv4:127.0.0.1";

    private static final String TARGET_2 = "nodename:example";

    @Test
    public void testGetType() {
        GrainTarget underTest = new GrainTarget(TARGET_1);
        assertEquals("grain", underTest.getType());
    }

    @Test
    public void testGetTarget() {
        GrainTarget underTest = new GrainTarget(TARGET_1);
        assertEquals(TARGET_1, underTest.getTarget());
        underTest = new GrainTarget(TARGET_2);
        assertEquals(TARGET_2, underTest.getTarget());
    }
}