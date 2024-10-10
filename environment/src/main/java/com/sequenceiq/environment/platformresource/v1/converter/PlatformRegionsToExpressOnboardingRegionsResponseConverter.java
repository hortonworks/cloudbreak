package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProviderResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProvidersResponse;

@Component
public class PlatformRegionsToExpressOnboardingRegionsResponseConverter {

    public ExpressOnboardingCloudProvidersResponse convert(CloudRegions source) {
        ExpressOnboardingCloudProvidersResponse json = new ExpressOnboardingCloudProvidersResponse();
        List<ExpressOnboardingCloudProviderResponse> regions = new ArrayList<>();

        for (Coordinate coordinate : source.getCoordinates().values()) {
            ExpressOnboardingCloudProviderResponse expressOnboardingRegionResponse = new ExpressOnboardingCloudProviderResponse();
            expressOnboardingRegionResponse.setLabel(coordinate.getDisplayName());
            expressOnboardingRegionResponse.setName(coordinate.getKey());

            expressOnboardingRegionResponse.setServices(coordinate.getCdpSupportedServices()
                    .stream()
                    .map(CdpSupportedServices::label)
                    .collect(Collectors.toList()));

            regions.add(expressOnboardingRegionResponse);
        }
        json.setRegions(regions);
        return json;
    }
}
