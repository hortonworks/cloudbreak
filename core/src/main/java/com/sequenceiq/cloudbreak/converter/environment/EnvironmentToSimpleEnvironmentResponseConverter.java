package com.sequenceiq.cloudbreak.converter.environment;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.response.LocationResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

@Component
public class EnvironmentToSimpleEnvironmentResponseConverter extends AbstractConversionServiceAwareConverter<Environment, SimpleEnvironmentResponse> {
    @Inject
    private RegionConverter regionConverter;

    @Override
    public SimpleEnvironmentResponse convert(Environment source) {
        SimpleEnvironmentResponse response = new SimpleEnvironmentResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setLocation(getConversionService().convert(source, LocationResponse.class));
        return response;
    }
}
