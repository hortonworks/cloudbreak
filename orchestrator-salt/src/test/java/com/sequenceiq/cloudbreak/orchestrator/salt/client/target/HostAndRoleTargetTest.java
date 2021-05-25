package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HostAndRoleTargetTest {

    @Test
    public void testGetType() {
        HostAndRoleTarget underTest = new HostAndRoleTarget("foo", List.of());
        assertEquals("compound", underTest.getType());
    }

    @Test
    public void testGetTarget() {
        HostAndRoleTarget underTest = new HostAndRoleTarget("foo", List.of("example1.com", "example2.com"));
        assertEquals("G@roles:foo and L@example1.com,example2.com", underTest.getTarget());
    }
}