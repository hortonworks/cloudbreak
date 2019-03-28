package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto;

import io.grpc.ManagedChannel;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class AuthorizationClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     */
    AuthorizationClient(ManagedChannel channel,
            String actorCrn) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
    }

    public void checkRight(String requestId, String userCrn, String right, String resource) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(right);
        AuthorizationProto.RightCheck.Builder rightCheckBuilder = AuthorizationProto.RightCheck.newBuilder().setRight(right);
        if (!StringUtils.isEmpty(resource)) {
            rightCheckBuilder.setResource(resource);
        }
        newStub(requestId).checkRight(
                AuthorizationProto.CheckRightRequest.newBuilder()
                        .setActorCrn(userCrn)
                        .setCheck(rightCheckBuilder.build())
                        .build()
        );
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private AuthorizationGrpc.AuthorizationBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return AuthorizationGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
