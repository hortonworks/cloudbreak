package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

class EventGenerationIdsCheckerTest {
    private EventGenerationIdsChecker underTest = new EventGenerationIdsChecker();

    @Test
    void testIsNotInSync() {
        Stack stack = UserSyncTestUtils.createStack();
        UmsEventGenerationIds currentEventGenerationIds = UserSyncTestUtils.createUniqueUmsEventGenerationIds();
        UserSyncStatus userSyncStatus = UserSyncTestUtils.createUserSyncStatus(stack);
        userSyncStatus.setUmsEventGenerationIds(new Json(UserSyncTestUtils.createUniqueUmsEventGenerationIds()));

        assertFalse(underTest.isInSync(userSyncStatus, currentEventGenerationIds));
    }

    @Test
    void testIsInSync() {
        Stack stack = UserSyncTestUtils.createStack();
        UmsEventGenerationIds currentEventGenerationIds = UserSyncTestUtils.createUniqueUmsEventGenerationIds();
        UserSyncStatus userSyncStatus = UserSyncTestUtils.createUserSyncStatus(stack);
        userSyncStatus.setUmsEventGenerationIds(new Json(currentEventGenerationIds));

        assertTrue(underTest.isInSync(userSyncStatus, currentEventGenerationIds));
    }
}