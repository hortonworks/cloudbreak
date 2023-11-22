package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto.RightCheck;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;

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
     * @param rightChecks    right checks for the environment
     * @throws NullPointerException if the environmentCrn is null
     * @throws CrnParseException    if the environmentCrn does not match the CRN pattern or cannot be parsed
     */
    public EnvironmentAccessChecker(GrpcUmsClient grpcUmsClient, String environmentCrn, List<RightCheck> rightChecks) {
        this.grpcUmsClient = requireNonNull(grpcUmsClient, "grpcUmsClient is null");
        Crn.safeFromString(environmentCrn);
        this.environmentCrn = environmentCrn;
        this.rightChecks = requireNonNull(rightChecks);
        checkArgument(rightChecks.size() == UserSyncConstants.RIGHTS.size());
    }

    public EnvironmentAccessRights hasAccess(String memberCrn) {
        requireNonNull(memberCrn, "memberCrn is null");
        try {
            List<Boolean> hasRights = grpcUmsClient.hasRightsNoCache(memberCrn, rightChecks);
            return new EnvironmentAccessRights(hasRights.get(0), hasRights.get(1));
        } catch (UnauthorizedException e) {
            LOGGER.warn("Member CRN {} not found in UMS. Treating as if member has no rights to environment {}: {}",
                    memberCrn, environmentCrn, e.getLocalizedMessage());
            return new EnvironmentAccessRights(false, false);
        }
    }
}
