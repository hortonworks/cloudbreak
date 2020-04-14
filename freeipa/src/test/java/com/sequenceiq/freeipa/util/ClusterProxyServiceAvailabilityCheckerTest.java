package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.freeipa.entity.Stack;

public class ClusterProxyServiceAvailabilityCheckerTest {

    @Test
    public void testisBalancedDnsAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.20.0");
        assertTrue(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.20.0-rc.51");
        assertTrue(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.21.0-rc.1");
        assertTrue(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));
    }

    @Test
    public void testisBalancedDnsUnavailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.20.0-rc.50");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.20.0-dev.2");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.19.0");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.19.0-rc.23");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.19.0-rc.122");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.19.0-dev.23");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.18.0");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.18.0-rc.2");
        assertFalse(ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack));
    }

}