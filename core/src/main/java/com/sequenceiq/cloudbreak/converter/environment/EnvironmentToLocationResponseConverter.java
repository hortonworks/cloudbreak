package com.sequenceiq.cloudbreak.converter.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.response.LocationResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentToLocationResponseConverter extends AbstractConversionServiceAwareConverter<Environment, LocationResponse> {

    @Override
    public LocationResponse convert(Environment environment) {
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setLatitude(environment.getLatitude());
        locationResponse.setLongitude(environment.getLongitude());
        locationResponse.setLocationName(environment.getLocation());
        locationResponse.setLocationDisplayName(environment.getLocationDisplayName());
        return locationResponse;
    }
}
