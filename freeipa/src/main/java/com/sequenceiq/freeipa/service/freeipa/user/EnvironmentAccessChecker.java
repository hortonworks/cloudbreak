package com.sequenceiq.freeipa.service.freeipa.user;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class EnvironmentAccessChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentAccessChecker.class);

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private static final String ACCESS_ENVIRONMENT_RIGHT = RightUtils.getRight(AuthorizationResource.ENVIRONMENT, ResourceAction.ACCESS_ENVIRONMENT);

    private static final String ADMIN_FREEIPA_RIGHT = RightUtils.getRight(AuthorizationResource.ENVIRONMENT, ResourceAction.ADMIN_FREEIPA);

    private final GrpcUmsClient grpcUmsClient;

    private final String environmentCrn;

    private final List<AuthorizationProto.RightCheck> rightChecks;

    /**
     * Creates an EnvironmentAccessChecker instance.
     * @param grpcUmsClient a GrpcUmsClient
     * @param environmentCrn a environment CRN
     * @throws NullPointerException if the environmentCrn is null
     * @throws CrnParseException    if the environmentCrn does not match the CRN pattern or cannot be parsed
     */
    public EnvironmentAccessChecker(GrpcUmsClient grpcUmsClient, String environmentCrn) {
        this.grpcUmsClient = requireNonNull(grpcUmsClient, "grpcUmsClient is null");
        Crn.safeFromString(environmentCrn);
        this.environmentCrn = environmentCrn;

        this.rightChecks = List.of(
                AuthorizationProto.RightCheck.newBuilder()
                        .setRight(ACCESS_ENVIRONMENT_RIGHT)
                        .setResource(environmentCrn)
                        .build(),
                AuthorizationProto.RightCheck.newBuilder()
                        .setRight(ADMIN_FREEIPA_RIGHT)
                        .build());
    }

    public EnvironmentAccessRights hasAccess(String memberCrn, Optional<String> requestId) {
        requireNonNull(memberCrn, "memberCrn is null");
        requireNonNull(requestId, "requestId is null");

        try {
            List<Boolean> hasRights = grpcUmsClient.hasRights(IAM_INTERNAL_ACTOR_CRN, memberCrn, rightChecks, requestId);
            return new EnvironmentAccessRights(hasRights.get(0), hasRights.get(1));
        } catch (StatusRuntimeException e) {
            // NOT_FOUND errors indicate that a user/machineUser has been deleted after we have
            // retrieved the list of users/machineUsers from the UMS. Treat these users as if
            // they do not have the right to access this environment and belong to no groups.
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                LOGGER.warn("Member CRN {} not found in UMS. Treating as if member has no rights to environment {}: {}",
                        memberCrn, environmentCrn, e.getLocalizedMessage());
                return new EnvironmentAccessRights(false, false);
            } else {
                throw e;
            }

        }
    }
}
