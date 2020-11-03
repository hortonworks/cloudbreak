package com.sequenceiq.freeipa.util;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class HealthCheckAvailabilityCheckerTest {

    private static final String AVAILABLE_VERSION = "2.32.0";

    private static final String UNAVAILABLE_VERSION = "2.31.0";

    private static final String ACCOUNT_ID = "account-id";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private HealthCheckAvailabilityChecker underTest;

    @Test
    public void testAvailable() {
        when(entitlementService.freeIpaHealthCheckEnabled(any(), any())).thenReturn(true);
        Stack stack = new Stack();

        stack.setAppVersion(AVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);
        assertTrue(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        verify(entitlementService).freeIpaHealthCheckEnabled(eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID));
    }

    @Test
    public void testUnavailableVersion() {
        Stack stack = new Stack();

        stack.setAppVersion(UNAVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));
    }

    @Test
    public void testUnavailableEntitlement() {
        when(entitlementService.freeIpaHealthCheckEnabled(any(), any())).thenReturn(false);
        Stack stack = new Stack();

        stack.setAppVersion(AVAILABLE_VERSION);
        stack.setAccountId(ACCOUNT_ID);
        assertFalse(underTest.isCdpFreeIpaHeathAgentAvailable(stack));

        verify(entitlementService).freeIpaHealthCheckEnabled(eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID));
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