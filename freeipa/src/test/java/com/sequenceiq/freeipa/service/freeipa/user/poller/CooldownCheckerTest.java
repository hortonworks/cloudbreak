package com.sequenceiq.freeipa.service.freeipa.user.poller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;

class CooldownCheckerTest {

    private CooldownChecker underTest = new CooldownChecker();

    @Test
    void testIsCool() throws Exception {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() - 1L;
        userSyncStatus.setLastFullSyncStartTime(lastStartTime);

        assertTrue(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsNotCool() throws Exception {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() + 1L;
        userSyncStatus.setLastFullSyncStartTime(lastStartTime);

        assertFalse(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

}