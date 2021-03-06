package com.sequenceiq.freeipa.service.freeipa.user;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

public class UserSyncTestUtils {
    public static final String ACCOUNT_ID = UUID.randomUUID().toString();

    public static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private UserSyncTestUtils() {
    }

    public static Stack createStack() {
        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stack;
    }

    public static UserSyncStatus createUserSyncStatus(Stack stack) {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        userSyncStatus.setStack(stack);
        return userSyncStatus;
    }

    public static UmsEventGenerationIds createUniqueUmsEventGenerationIds() {
        UmsEventGenerationIds umsEventGenerationIds = new UmsEventGenerationIds();
        umsEventGenerationIds.setEventGenerationIds(Map.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        return umsEventGenerationIds;
    }

    public static WorkloadCredential createWorkloadCredential(String hashedPassword, long credentialsVersion) {
        return new WorkloadCredential(hashedPassword,
                List.of(),
                Optional.of(Instant.now()),
                List.of(UserManagementProto.SshPublicKey.newBuilder().setPublicKey("fakepublickey").build(),
                        UserManagementProto.SshPublicKey.newBuilder().setPublicKey("anotherfakepublickey").build()),
                credentialsVersion);
    }
}
