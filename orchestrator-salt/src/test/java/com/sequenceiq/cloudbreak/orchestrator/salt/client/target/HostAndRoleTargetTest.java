package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testEquals() {
        HostAndRoleTarget test1 = new HostAndRoleTarget("foo", List.of("example1.com", "example2.com"));
        HostAndRoleTarget test2 = new HostAndRoleTarget("foo", List.of("example1.com", "example2.com"));
        assertEquals(test1, test2);
    }

    @Test
    public void testNotEqualsDifferentRole() {
        HostAndRoleTarget test1 = new HostAndRoleTarget("foo", List.of("example1.com", "example2.com"));
        HostAndRoleTarget test2 = new HostAndRoleTarget("bar", List.of("example1.com", "example2.com"));
        assertNotEquals(test1, test2);
    }

    @Test
    public void testNotEqualsDifferentHost() {
        HostAndRoleTarget test1 = new HostAndRoleTarget("foo", List.of("example1.com", "example2.com"));
        HostAndRoleTarget test2 = new HostAndRoleTarget("foo", List.of("example1.com", "example3.com"));
        assertNotEquals(test1, test2);
    }

    @Test
    public void testNotEqualsMoreHost() {
        HostAndRoleTarget test1 = new HostAndRoleTarget("foo", List.of("example1.com", "example2.com"));
        HostAndRoleTarget test2 = new HostAndRoleTarget("bar", List.of("example1.com", "example2.com", "example3.com"));
        assertNotEquals(test1, test2);
    }
}