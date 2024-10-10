package com.sequenceiq.environment.expressonboarding.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetCdpPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProvidersResponse;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.v1.converter.PlatformRegionsToExpressOnboardingRegionsResponseConverter;

@Service
public class ExpressOnboardingRegionsResponseConverter {

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private PlatformRegionsToExpressOnboardingRegionsResponseConverter platformRegionsToExpressOnboardingRegionsResponseConverter;

    public Map<String, ExpressOnboardingCloudProvidersResponse> expressOnboardingRegionsResponse() {
        Map<String, ExpressOnboardingCloudProvidersResponse> detailedExpressOnboardingRegionsResponse = new HashMap<>();

        for (CloudPlatform publicCloudPlatform : CloudPlatform.publicCloudPlatforms()) {
            String platform = publicCloudPlatform.name();
            GetCdpPlatformRegionsRequest request = platformParameterService.getCdpPlatformRegionsRequestV2(
                    platform,
                    platform);
            CloudRegions regions = platformParameterService.getCdpRegions(request);
            detailedExpressOnboardingRegionsResponse.put(
                    platform.toUpperCase(),
                    platformRegionsToExpressOnboardingRegionsResponseConverter.convert(regions));
        }

        return detailedExpressOnboardingRegionsResponse;
    }
}
