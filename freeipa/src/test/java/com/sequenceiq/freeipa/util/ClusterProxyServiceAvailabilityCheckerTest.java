package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class ClusterProxyServiceAvailabilityCheckerTest {

    private ClusterProxyServiceAvailabilityChecker underTest;

    @Before
    public void before() {
        underTest = new ClusterProxyServiceAvailabilityChecker();
    }

    @Test
    public void testisBalancedDnsAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.21.0-rc.1");
        assertTrue(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.22.0-rc.1");
        assertTrue(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.21.0");
        assertTrue(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.21.0-dev.2");
        assertTrue(underTest.isDnsBasedServiceNameAvailable(stack));
    }

    @Test
    public void testisBalancedDnsUnavailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.20.0");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.20.0-rc.23");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.20.0-rc.122");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.20.0-dev.23");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.19.0");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("2.19.0-rc.2");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));
    }

    @Test
    public void testAppVersionIsBlank() {
        Stack stack = new Stack();
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion("");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));

        stack.setAppVersion(" ");
        assertFalse(underTest.isDnsBasedServiceNameAvailable(stack));
    }

}