package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

import org.springframework.stereotype.Component;

import javax.inject.Inject;

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
