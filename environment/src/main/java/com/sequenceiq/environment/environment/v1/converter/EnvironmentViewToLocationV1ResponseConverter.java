package com.sequenceiq.environment.environment.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.environment.v1.model.response.LocationResponse;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Component
public class EnvironmentViewToLocationV1ResponseConverter extends AbstractConversionServiceAwareConverter<EnvironmentView, LocationResponse> {

    @Override
    public LocationResponse convert(EnvironmentView environmentView) {
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setLatitude(environmentView.getLatitude());
        locationResponse.setLongitude(environmentView.getLongitude());
        locationResponse.setName(environmentView.getLocation());
        locationResponse.setDisplayName(environmentView.getLocationDisplayName());
        return locationResponse;
    }
}
