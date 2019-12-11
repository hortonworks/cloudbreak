package com.sequenceiq.freeipa.service.freeipa.user.poller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class UserSyncPollerEntitlementCheckerTest {
    @Mock
    EntitlementService entitlementService;

    @InjectMocks
    UserSyncPollerEntitlementChecker underTest;

    @Test
    void accountIsNotEntitled() {
        when(entitlementService.automaticUsersyncPollerEnabled(anyString(), anyString())).thenReturn(false);

        assertFalse(underTest.isAccountEntitled(UserSyncPollerTestUtils.ACCOUNT_ID));
    }

    @Test
    void accountIsEntitled() {
        when(entitlementService.automaticUsersyncPollerEnabled(anyString(), anyString())).thenReturn(true);

        assertTrue(underTest.isAccountEntitled(UserSyncPollerTestUtils.ACCOUNT_ID));
    }
}