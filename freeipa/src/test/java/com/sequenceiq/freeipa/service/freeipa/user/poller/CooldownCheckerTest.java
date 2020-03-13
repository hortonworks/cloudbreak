package com.sequenceiq.freeipa.service.freeipa.user.poller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;

class CooldownCheckerTest {

    private CooldownChecker underTest = new CooldownChecker();

    @Test
    void testNullThrows() {
        assertThrows(NullPointerException.class, () -> underTest.isCooldownExpired(null, Instant.now()));
    }

    @Test
    void testNoStartTimeIsCool() {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();

        assertTrue(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsCool() {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() - 1L;
        Operation lastRequestedOperation = new Operation();
        lastRequestedOperation.setStartTime(lastStartTime);
        userSyncStatus.setLastStartedFullSync(lastRequestedOperation);

        assertTrue(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsNotCool() {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() + 1L;
        Operation lastRequestedOperation = new Operation();
        lastRequestedOperation.setStartTime(lastStartTime);
        userSyncStatus.setLastStartedFullSync(lastRequestedOperation);

        assertFalse(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsCoolDeprecated() {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() - 1L;
        userSyncStatus.setLastFullSyncStartTime(lastStartTime);

        assertTrue(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsNotCoolDeprecated() {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long lastStartTime = cooldownExpiration.toEpochMilli() + 1L;
        userSyncStatus.setLastFullSyncStartTime(lastStartTime);

        assertFalse(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }

    @Test
    void testIsCoolPrefersOperation() {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        Instant cooldownExpiration = Instant.now();
        long coolTime = cooldownExpiration.toEpochMilli() - 1L;
        long notCoolTime = cooldownExpiration.toEpochMilli() + 1L;
        Operation lastRequestedOperation = new Operation();
        lastRequestedOperation.setStartTime(coolTime);
        userSyncStatus.setLastStartedFullSync(lastRequestedOperation);
        userSyncStatus.setLastFullSyncStartTime(notCoolTime);

        assertTrue(underTest.isCooldownExpired(userSyncStatus, cooldownExpiration));
    }
}