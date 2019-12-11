package com.sequenceiq.freeipa.service.freeipa.user.poller;

import java.util.Map;
import java.util.UUID;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

public class UserSyncPollerTestUtils {
    static final String ACCOUNT_ID = UUID.randomUUID().toString();

    static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private UserSyncPollerTestUtils() {
    }

    static Stack createStack() {
        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stack;
    }

    static UserSyncStatus createUserSyncStatus(Stack stack) {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        userSyncStatus.setStack(stack);
        return userSyncStatus;
    }

    static UmsEventGenerationIds createUniqueUmsEventGenerationIds() {
        UmsEventGenerationIds umsEventGenerationIds = new UmsEventGenerationIds();
        umsEventGenerationIds.setEventGenerationIds(Map.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        return umsEventGenerationIds;
    }
}
