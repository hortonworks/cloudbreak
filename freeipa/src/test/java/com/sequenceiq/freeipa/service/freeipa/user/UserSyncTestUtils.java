package com.sequenceiq.freeipa.service.freeipa.user;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

public class UserSyncTestUtils {
    public static final String ACCOUNT_ID = UUID.randomUUID().toString();

    public static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    public static final Crn ENV_CRN = Crn.fromString(ENVIRONMENT_CRN);

    public static final Long STACK_ID = 3L;

    private UserSyncTestUtils() {
    }

    public static Stack createStack() {
        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stack;
    }

    public static UserSyncStatus createUserSyncStatus() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        userSyncStatus.setStackId(STACK_ID);
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
