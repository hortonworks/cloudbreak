package com.sequenceiq.environment.environment.v1.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentToSimpleEnvironmentResponseConverter extends AbstractConversionServiceAwareConverter<Environment, SimpleEnvironmentResponse> {
    @Inject
    private RegionConverter regionConverter;

    @Override
    public SimpleEnvironmentResponse convert(Environment source) {
        SimpleEnvironmentResponse response = new SimpleEnvironmentResponse();
        response.setId(source.getResourceCrn());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setLocation(getConversionService().convert(source, LocationResponse.class));
        response.setCredentialName(source.getCredential().getName());
        return response;
    }
}
