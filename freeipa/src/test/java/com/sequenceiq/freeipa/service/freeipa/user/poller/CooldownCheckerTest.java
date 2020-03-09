package com.sequenceiq.freeipa.service.freeipa.user.poller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;

class CooldownCheckerTest {

    private CooldownChecker underTest = new CooldownChecker();

    @Test
    void testIsCoolNoPreviousSync() throws Exception {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();

        assertTrue(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsCoolOldPreviousSync() throws Exception {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() - 1L;
        Operation lastRequestedOperation = mock(Operation.class);
        when(lastRequestedOperation.getStartTime()).thenReturn(lastStartTime);
        userSyncStatus.setLastRequestedFullSync(lastRequestedOperation);

        assertTrue(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsNotCool() throws Exception {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() + 1L;
        Operation lastRequestedOperation = mock(Operation.class);
        when(lastRequestedOperation.getStartTime()).thenReturn(lastStartTime);
        userSyncStatus.setLastRequestedFullSync(lastRequestedOperation);

        assertFalse(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

}