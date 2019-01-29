package com.sequenceiq.cloudbreak.converter.v4.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.LocationV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentToLocationV4ResponseConverter extends AbstractConversionServiceAwareConverter<Environment, LocationV4Response> {

    @Override
    public LocationV4Response convert(Environment environment) {
        LocationV4Response locationV4Response = new LocationV4Response();
        locationV4Response.setLatitude(environment.getLatitude());
        locationV4Response.setLongitude(environment.getLongitude());
        locationV4Response.setName(environment.getLocation());
        locationV4Response.setDisplayName(environment.getLocationDisplayName());
        return locationV4Response;
    }
}
