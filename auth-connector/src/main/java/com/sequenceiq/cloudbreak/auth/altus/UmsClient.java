package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc.UserManagementBlockingStub;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;

import io.grpc.ManagedChannel;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class UmsClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     */
    UmsClient(ManagedChannel channel,
            String actorCrn) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
    }

    /**
     * Wraps a call to getUser.
     *
     * @param requestId the request ID for the request
     * @param userCrn   the user CRN
     * @return the user
     */
    public User getUser(String requestId, String userCrn) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        return newStub(requestId).getUser(
                GetUserRequest.newBuilder()
                        .setAccountId(Crn.fromString(userCrn).getAccountId())
                        .setUserIdOrCrn(userCrn)
                        .build()
        ).getUser();
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private UserManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return UserManagementGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
