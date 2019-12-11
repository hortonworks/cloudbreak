package com.sequenceiq.freeipa.service.freeipa.user.poller;

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
    void testIsNotInSync() throws Exception {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UmsEventGenerationIds currentEventGenerationIds = UserSyncPollerTestUtils.createUniqueUmsEventGenerationIds();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        userSyncStatus.setUmsEventGenerationIds(new Json(UserSyncPollerTestUtils.createUniqueUmsEventGenerationIds()));

        assertFalse(underTest.isInSync(userSyncStatus, currentEventGenerationIds));
    }

    @Test
    void testIsInSync() throws Exception {
        Stack stack = UserSyncPollerTestUtils.createStack();
        UmsEventGenerationIds currentEventGenerationIds = UserSyncPollerTestUtils.createUniqueUmsEventGenerationIds();
        UserSyncStatus userSyncStatus = UserSyncPollerTestUtils.createUserSyncStatus(stack);
        userSyncStatus.setUmsEventGenerationIds(new Json(currentEventGenerationIds));

        assertTrue(underTest.isInSync(userSyncStatus, currentEventGenerationIds));
    }
}