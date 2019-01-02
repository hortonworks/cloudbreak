package com.sequenceiq.cloudbreak.converter.v4.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.LocationV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@Component
public class EnvironmentViewToLocationV4ResponseConverter extends AbstractConversionServiceAwareConverter<EnvironmentView, LocationV4Response> {

    @Override
    public LocationV4Response convert(EnvironmentView environmentView) {
        LocationV4Response locationV4Response = new LocationV4Response();
        locationV4Response.setLatitude(environmentView.getLatitude());
        locationV4Response.setLongitude(environmentView.getLongitude());
        locationV4Response.setLocationName(environmentView.getLocation());
        return locationV4Response;
    }
}
