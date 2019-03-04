package com.sequenceiq.cloudbreak.converter.v4.environment;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.LocationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class EnvironmentToSimpleEnvironmentV4ResponseConverter extends AbstractConversionServiceAwareConverter<Environment, SimpleEnvironmentV4Response> {
    @Inject
    private RegionConverter regionConverter;

    @Override
    public SimpleEnvironmentV4Response convert(Environment source) {
        SimpleEnvironmentV4Response response = new SimpleEnvironmentV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        response.setLocation(getConversionService().convert(source, LocationV4Response.class));
        response.setCredentialName(source.getCredential().getName());
        response.setWorkloadClusterNames(
                source.getStacks()
                        .stream()
                        .filter(stack -> stack.getType() == StackType.WORKLOAD)
                        .map(CompactView::getName)
                        .collect(Collectors.toSet()));
        response.setDatalakeClusterNames(
                source.getStacks()
                        .stream()
                        .filter(stack -> stack.getType() == StackType.DATALAKE)
                        .map(CompactView::getName)
                        .collect(Collectors.toSet()));
        return response;
    }
}
