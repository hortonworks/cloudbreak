package com.sequenceiq.environment.environment.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.environment.model.response.LocationV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Response;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentToSimpleEnvironmentV1ResponseConverter extends AbstractConversionServiceAwareConverter<Environment, SimpleEnvironmentV1Response> {
    @Inject
    private RegionConverter regionConverter;

    @Override
    public SimpleEnvironmentV1Response convert(Environment source) {
        SimpleEnvironmentV1Response response = new SimpleEnvironmentV1Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setLocation(getConversionService().convert(source, LocationV1Response.class));
        response.setCredentialName(source.getCredential().getName());
        return response;
    }
}
