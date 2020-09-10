package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto.RightCheck;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

public class EnvironmentAccessChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentAccessChecker.class);

    private final GrpcUmsClient grpcUmsClient;

    private final String environmentCrn;

    private final List<RightCheck> rightChecks;

    /**
     * Creates an EnvironmentAccessChecker instance.
     *
     * @param grpcUmsClient  a GrpcUmsClient
     * @param environmentCrn a environment CRN
     * @throws NullPointerException if the environmentCrn is null
     * @throws CrnParseException    if the environmentCrn does not match the CRN pattern or cannot be parsed
     */
    public EnvironmentAccessChecker(GrpcUmsClient grpcUmsClient, UmsRightProvider umsRightProvider, String environmentCrn) {
        this.grpcUmsClient = requireNonNull(grpcUmsClient, "grpcUmsClient is null");
        Crn.safeFromString(environmentCrn);
        this.environmentCrn = environmentCrn;

        rightChecks = createRightCheck(umsRightProvider, environmentCrn);
    }

    public EnvironmentAccessRights hasAccess(String memberCrn, Optional<String> requestId) {
        requireNonNull(memberCrn, "memberCrn is null");
        requireNonNull(requestId, "requestId is null");

        try {
            List<Boolean> hasRights = grpcUmsClient.hasRights(INTERNAL_ACTOR_CRN, memberCrn, rightChecks, requestId);
            return new EnvironmentAccessRights(hasRights.get(0), hasRights.get(1));
        } catch (StatusRuntimeException e) {
            // NOT_FOUND errors indicate that a user/machineUser has been deleted after we have
            // retrieved the list of users/machineUsers from the UMS. Treat these users as if
            // they do not have the right to access this environment and belong to no groups.
            if (e.getStatus().getCode() == Code.NOT_FOUND) {
                LOGGER.warn("Member CRN {} not found in UMS. Treating as if member has no rights to environment {}: {}",
                        memberCrn, environmentCrn, e.getLocalizedMessage());
                return new EnvironmentAccessRights(false, false);
            } else {
                throw e;
            }

        }
    }

    @VisibleForTesting
    static List<RightCheck> createRightCheck(UmsRightProvider umsRightProvider, String environmentCrn) {
        String accountId = Crn.safeFromString(environmentCrn).getAccountId();

        return List.of(
                RightCheck.newBuilder()
                        .setRight(umsRightProvider.getRight(AuthorizationResourceAction.ACCESS_ENVIRONMENT, INTERNAL_ACTOR_CRN, accountId))
                        .setResource(environmentCrn)
                        .build(),
                RightCheck.newBuilder()
                        .setRight(umsRightProvider.getRight(AuthorizationResourceAction.ADMIN_FREEIPA, INTERNAL_ACTOR_CRN, accountId))
                        .build());
    }
}
