package com.sequenceiq.cloudbreak.converter.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.response.LocationResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@Component
public class EnvironmentViewToLocationResponseConverter extends AbstractConversionServiceAwareConverter<EnvironmentView, LocationResponse> {

    @Override
    public LocationResponse convert(EnvironmentView environmentView) {
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setLatitude(environmentView.getLatitude());
        locationResponse.setLongitude(environmentView.getLongitude());
        locationResponse.setLocationName(environmentView.getLocation());
        locationResponse.setLocationDisplayName(environmentView.getLocationDisplayName());
        return locationResponse;
    }
}
