package com.sequenceiq.environment.environment.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentToLocationRequestConverter extends AbstractConversionServiceAwareConverter<Environment, LocationRequest> {
    @Override
    public LocationRequest convert(Environment source) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName(source.getLocation());
        locationRequest.setLatitude(source.getLatitude());
        locationRequest.setLongitude(source.getLongitude());
        return locationRequest;
    }
}
