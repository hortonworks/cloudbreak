package com.sequenceiq.environment.environment.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.environment.model.response.LocationV1Response;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentToLocationV1ResponseConverter extends AbstractConversionServiceAwareConverter<Environment, LocationV1Response> {

    @Override
    public LocationV1Response convert(Environment environment) {
        LocationV1Response locationV1Response = new LocationV1Response();
        locationV1Response.setLatitude(environment.getLatitude());
        locationV1Response.setLongitude(environment.getLongitude());
        locationV1Response.setName(environment.getLocation());
        locationV1Response.setDisplayName(environment.getLocationDisplayName());
        return locationV1Response;
    }
}
