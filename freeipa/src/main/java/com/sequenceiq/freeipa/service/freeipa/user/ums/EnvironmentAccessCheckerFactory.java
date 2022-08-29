package com.sequenceiq.freeipa.service.freeipa.user.ums;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@Component
public class EnvironmentAccessCheckerFactory {
    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private AuthorizationRightChecksFactory authorizationRightChecksFactory;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public EnvironmentAccessChecker create(String environmentCrn) {
        return new EnvironmentAccessChecker(
                grpcUmsClient, environmentCrn,
                authorizationRightChecksFactory.create(environmentCrn), regionAwareInternalCrnGeneratorFactory);
    }
}
