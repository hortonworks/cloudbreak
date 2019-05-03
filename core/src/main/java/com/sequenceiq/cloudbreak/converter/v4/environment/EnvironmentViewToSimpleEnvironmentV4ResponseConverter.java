package com.sequenceiq.cloudbreak.converter.v4.environment;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.EnvironmentNetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.LocationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@Component
public class EnvironmentViewToSimpleEnvironmentV4ResponseConverter extends
        AbstractConversionServiceAwareConverter<EnvironmentView, SimpleEnvironmentV4Response> {
    @Inject
    private RegionConverter regionConverter;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Override
    public SimpleEnvironmentV4Response convert(EnvironmentView source) {
        SimpleEnvironmentV4Response response = new SimpleEnvironmentV4Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        response.setLocation(getConversionService().convert(source, LocationV4Response.class));
        if (source.getDatalakeResources() != null) {
            response.setDatalakeResourcesNames(source.getDatalakeResources()
                .stream()
                .map(DatalakeResources::getName)
                .collect(Collectors.toSet()));
        }
        setNetworkIfPossible(response, source);
        return response;
    }

    private void setNetworkIfPossible(SimpleEnvironmentV4Response response, EnvironmentView source) {
        BaseNetwork network = source.getNetwork();
        if (network != null) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(source.getCloudPlatform());
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(cloudPlatform);
            if (environmentNetworkConverter != null) {
                EnvironmentNetworkV4Response networkV4Response = environmentNetworkConverter.convert(source.getNetwork());
                response.setNetwork(networkV4Response);
            }
        }
    }
}
