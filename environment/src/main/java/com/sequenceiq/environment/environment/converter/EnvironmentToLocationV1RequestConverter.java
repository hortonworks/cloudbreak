package com.sequenceiq.environment.environment.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.environment.model.request.LocationV1Request;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentToLocationV1RequestConverter extends AbstractConversionServiceAwareConverter<Environment, LocationV1Request> {
    @Override
    public LocationV1Request convert(Environment source) {
        LocationV1Request locationRequest = new LocationV1Request();
        locationRequest.setName(source.getLocation());
        locationRequest.setLatitude(source.getLatitude());
        locationRequest.setLongitude(source.getLongitude());
        return locationRequest;
    }
}
