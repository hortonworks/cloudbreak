package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class HealthCheckAvailabilityCheckerTest {

    private HealthCheckAvailabilityChecker underTest;

    @Before
    public void before() {
        underTest = new HealthCheckAvailabilityChecker();
    }

    @Test
    public void testHealthCheckAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.29.0-b1");
        assertTrue(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.30.0-b1");
        assertTrue(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.29.0");
        assertTrue(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.29.0-b2");
        assertTrue(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    @Test
    public void testHealthCheckUnavailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.28.0");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.28.0-b23");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.28.0-b122");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.28.0-b23");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.27.0");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("2.27.0-b2");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    @Test
    public void testAppVersionIsBlank() {
        Stack stack = new Stack();
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion("");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        stack.setAppVersion(" ");
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

}