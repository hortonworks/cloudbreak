package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class HealthCheckAvailabilityCheckerTest {

    private static final String AVAILABLE_VERSION = "2.32.0";

    private static final String UNAVAILABLE_VERSION = "2.31.0";

    private static final String ACCOUNT_ID = "account-id";

    @InjectMocks
    private HealthCheckAvailabilityChecker underTest;

    @Test
    public void testAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion(AVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);
        assertTrue(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    @Test
    public void testUnavailableVersion() {
        Stack stack = new Stack();

        stack.setAppVersion(UNAVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);
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