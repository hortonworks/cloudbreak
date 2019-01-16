package com.sequenceiq.cloudbreak.converter.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.request.LocationRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentToLocationRequestConverter extends AbstractConversionServiceAwareConverter<Environment, LocationRequest> {
    @Override
    public LocationRequest convert(Environment source) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName(source.getLocation());
        locationRequest.setLatitude(source.getLatitude());
        locationRequest.setLongitude(source.getLongitude());
        return locationRequest;
    }
}
