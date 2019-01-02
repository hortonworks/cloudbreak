package com.sequenceiq.cloudbreak.converter.v4.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.LocationV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentToLocationV4RequestConverter extends AbstractConversionServiceAwareConverter<Environment, LocationV4Request> {
    @Override
    public LocationV4Request convert(Environment source) {
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setLocationName(source.getLocation());
        locationRequest.setLatitude(source.getLatitude());
        locationRequest.setLongitude(source.getLongitude());
        return locationRequest;
    }
}
