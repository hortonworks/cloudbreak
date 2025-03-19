package com.sequenceiq.cloudbreak.sdx.pdl.service;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_HYBRID_CLOUD;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxCommonService;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

public abstract class AbstractPdlSdxService implements PlatformAwareSdxCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPdlSdxService.class);

    @Value("${sdx.pdl.enabled:false}")
    private boolean pdlEnabled;

    @Inject
    private EntitlementService entitlementService;

    @Lazy
    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Lazy
    @Inject
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @Override
    public TargetPlatform targetPlatform() {
        return TargetPlatform.PDL;
    }

    @Override
    public boolean isPlatformEntitled(String accountId) {
        return entitlementService.isEntitledFor(accountId, CDP_HYBRID_CLOUD);
    }

    public boolean isEnabled(String crn) {
        boolean enabled = pdlEnabled && isPlatformEntitled(Crn.safeFromString(crn).getAccountId());
        if (!pdlEnabled) {
            LOGGER.debug("PDL is not enabled. {} {}",
                    pdlEnabled, isPlatformEntitled(Crn.safeFromString(crn).getAccountId()));
        }
        return enabled;
    }

    public Environment getPrivateEnvForPublicEnv(String publicEnvCrn) {
        String pvcCrn = getPrivateCloudEnvCrn(publicEnvCrn);
        if (!StringUtils.isEmpty(pvcCrn)) {
            DescribeRemoteEnvironment describeRemoteEnvironment = new DescribeRemoteEnvironment();
            describeRemoteEnvironment.setCrn(pvcCrn);
            return remoteEnvironmentEndpoint.getByCrn(describeRemoteEnvironment).getEnvironment();
        }
        return null;
    }

    public String getPrivateCloudEnvCrn(String publicEnvCrn) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = environmentEndpoint.getByCrn(publicEnvCrn);
        // change it later to fetch from detailedEnvironmentResponse
        //return "crn:altus:environments:us-west-1:69159fae-78cd-4427-b942-aec2676a4dd6:environment:test-env-1/28e35068-c9f6-4a21-b1d7-55f51ac3076d";
        return null;
    }
}