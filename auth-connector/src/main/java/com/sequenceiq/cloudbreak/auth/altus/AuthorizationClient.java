package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentracing.Tracer;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class AuthorizationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationClient.class);

    private final ManagedChannel channel;

    private final Tracer tracer;

    private final UmsClientConfig umsClientConfig;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param tracer   tracer
     */
    AuthorizationClient(ManagedChannel channel, UmsClientConfig umsClientConfig, Tracer tracer,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.umsClientConfig = checkNotNull(umsClientConfig, "umsClientConfig should not be null.");
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
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
        try {
            AuthorizationProto.HasRightsResponse response = newStub(requestId).hasRights(
                    AuthorizationProto.HasRightsRequest.newBuilder()
                            .setActorCrn(actorCrn)
                            .addAllCheck(rightChecks)
                            .build()
            );
            return response.getResultList();
        } catch (StatusRuntimeException statusRuntimeException) {
            if (Status.Code.DEADLINE_EXCEEDED.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("Deadline exceeded for hasRights {} for actor {} and rights {}", actorCrn, rightChecks, statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call timed out.");
            } else {
                LOGGER.error("Status runtime exception while checking hasRights {} for actor {} and rights {}", actorCrn, rightChecks, statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unkown error while checking hasRights {} for actor {} and rights {}", actorCrn, rightChecks, e);
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
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
                GrpcUtil.getTimeoutInterceptor(umsClientConfig.getGrpcShortTimeoutSec()),
                GrpcUtil.getTracingInterceptor(tracer),
                new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()),
                new CallingServiceNameInterceptor(umsClientConfig.getCallingServiceName())
        );
    }
}
