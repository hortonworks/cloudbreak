package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class AuthorizationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationClient.class);

    private final ManagedChannel channel;

    private final UmsClientConfig umsClientConfig;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    /**
     * Constructor.
     *
     * @param channel the managed channel.
     */
    AuthorizationClient(ManagedChannel channel, UmsClientConfig umsClientConfig,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.umsClientConfig = checkNotNull(umsClientConfig, "umsClientConfig should not be null.");
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void checkRight(String userCrn, String right, String resource) {
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(right, "right should not be null.");
        AuthorizationProto.RightCheck.Builder rightCheckBuilder = AuthorizationProto.RightCheck.newBuilder().setRight(right);
        if (!StringUtils.isEmpty(resource)) {
            rightCheckBuilder.setResource(resource);
        }
        newStub().checkRight(
                AuthorizationProto.CheckRightRequest.newBuilder()
                        .setActorCrn(userCrn)
                        .setCheck(rightCheckBuilder.build())
                        .build()
        );
    }

    public List<Boolean> hasRights(String actorCrn, Iterable<AuthorizationProto.RightCheck> rightChecks) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(rightChecks, "rightChecks should not be null.");
        try {
            AuthorizationProto.HasRightsResponse response = newStub().hasRights(
                    AuthorizationProto.HasRightsRequest.newBuilder()
                            .setActorCrn(actorCrn)
                            .addAllCheck(rightChecks)
                            .build()
            );
            return response.getResultList();
        } catch (StatusRuntimeException statusRuntimeException) {
            if (Status.Code.DEADLINE_EXCEEDED.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("Deadline exceeded for hasRights for actor {} and rights {}", actorCrn, rightChecks, statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call timed out.");
            } else if (Status.Code.NOT_FOUND.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("NOT_FOUND for hasRights for actor {} and rights {}", actorCrn, rightChecks, statusRuntimeException);
                throw new UnauthorizedException("Authorization failed for user: " + actorCrn);
            } else {
                LOGGER.error("Status runtime exception while checking hasRights for actor {} and rights {}", actorCrn, rightChecks, statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unknown error while checking hasRights for actor {} and rights {}", actorCrn, rightChecks, e);
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @return the stub
     */
    private AuthorizationGrpc.AuthorizationBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return AuthorizationGrpc.newBlockingStub(channel).withInterceptors(
                GrpcUtil.getTimeoutInterceptor(umsClientConfig.getGrpcShortTimeoutSec()),
                new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()),
                new CallingServiceNameInterceptor(umsClientConfig.getCallingServiceName())
        );
    }
}
