package com.sequenceiq.environment.environment.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.environment.model.response.LocationV1Response;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Component
public class EnvironmentViewToLocationV1ResponseConverter extends AbstractConversionServiceAwareConverter<EnvironmentView, LocationV1Response> {

    @Override
    public LocationV1Response convert(EnvironmentView environmentView) {
        LocationV1Response locationV1Response = new LocationV1Response();
        locationV1Response.setLatitude(environmentView.getLatitude());
        locationV1Response.setLongitude(environmentView.getLongitude());
        locationV1Response.setName(environmentView.getLocation());
        locationV1Response.setDisplayName(environmentView.getLocationDisplayName());
        return locationV1Response;
    }
}
