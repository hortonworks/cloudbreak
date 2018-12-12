package com.sequenceiq.cloudbreak.converter.environment;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.response.LocationResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@Component
public class EnvironmentViewToSimpleEnvironmentResponseConverter extends AbstractConversionServiceAwareConverter<EnvironmentView, SimpleEnvironmentResponse> {
    @Inject
    private RegionConverter regionConverter;

    @Override
    public SimpleEnvironmentResponse convert(EnvironmentView source) {
        SimpleEnvironmentResponse response = new SimpleEnvironmentResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        response.setLocation(getConversionService().convert(source, LocationResponse.class));
        if (source.getDatalakeResourcesId() != null) {
            response.setDatalakeResourcesName(source.getName());
        }
        return response;
    }
}
