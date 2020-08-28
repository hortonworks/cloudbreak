package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class BalancedDnsAvailabilityCheckerTest {

    private BalancedDnsAvailabilityChecker underTest;

    @Before
    public void before() {
        underTest = new BalancedDnsAvailabilityChecker();
    }

    @Test
    public void testBalancedDnsAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.20.0-rc.1");
        assertTrue(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.21.0-rc.1");
        assertTrue(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.20.0");
        assertTrue(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.20.0-dev.2");
        assertTrue(underTest.isBalancedDnsAvailable(stack));
    }

    @Test
    public void testBalancedDnsUnavailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.19.0");
        assertFalse(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.19.0-rc.23");
        assertFalse(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.19.0-rc.122");
        assertFalse(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.19.0-dev.23");
        assertFalse(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.18.0");
        assertFalse(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.18.0-rc.2");
        assertFalse(underTest.isBalancedDnsAvailable(stack));
    }

    @Test
    public void testAppVersionIsBlank() {
        Stack stack = new Stack();
        assertFalse(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion("");
        assertFalse(underTest.isBalancedDnsAvailable(stack));

        stack.setAppVersion(" ");
        assertFalse(underTest.isBalancedDnsAvailable(stack));
    }

}