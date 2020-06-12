package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RoleTargetTest {

    @Test
    public void testGetType() {
        RoleTarget underTest = new RoleTarget("foo");
        assertEquals("grain", underTest.getType());
    }

    @Test
    public void testGetTarget() {
        RoleTarget underTest = new RoleTarget("foo");
        assertEquals("roles:foo", underTest.getTarget());
    }
}