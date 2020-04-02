package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.freeipa.entity.Stack;

public class BalancedDnsAvailabilityCheckerTest {

    @Test
    public void testBalancedDnsAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.20.0-rc.1");
        assertTrue(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.21.0-rc.1");
        assertTrue(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.20.0");
        assertTrue(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.20.0-dev.2");
        assertTrue(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));
    }

    @Test
    public void testBalancedDnsUnavailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.19.0");
        assertFalse(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.19.0-rc.23");
        assertFalse(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.19.0-rc.122");
        assertFalse(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.19.0-dev.23");
        assertFalse(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.18.0");
        assertFalse(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));

        stack.setAppVersion("2.18.0-rc.2");
        assertFalse(BalancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack));
    }

}