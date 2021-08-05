package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.List;

import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class AuthorizationClient {

    private final ManagedChannel channel;

    private final Tracer tracer;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param tracer   tracer
     */
    AuthorizationClient(ManagedChannel channel, Tracer tracer) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.tracer = tracer;
    }

    public void checkRight(String requestId, String userCrn, String right, String resource) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(right, "right should not be null.");
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

    public List<Boolean> hasRights(String requestId, String actorCrn, Iterable<AuthorizationProto.RightCheck> rightChecks) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(rightChecks, "rightChecks should not be null.");
        AuthorizationProto.HasRightsResponse response = newStub(requestId).hasRights(
                AuthorizationProto.HasRightsRequest.newBuilder()
                        .setActorCrn(actorCrn)
                        .addAllCheck(rightChecks)
                        .build()
        );
        return response.getResultList();
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private AuthorizationGrpc.AuthorizationBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return AuthorizationGrpc.newBlockingStub(channel).withInterceptors(
                GrpcUtil.getTracingInterceptor(tracer),
                new AltusMetadataInterceptor(requestId, INTERNAL_ACTOR_CRN)
        );
    }
}
