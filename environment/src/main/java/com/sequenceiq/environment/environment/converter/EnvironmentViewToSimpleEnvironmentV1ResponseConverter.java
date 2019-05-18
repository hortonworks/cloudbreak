package com.sequenceiq.environment.environment.converter;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.response.EnvironmentNetworkV1Response;
import com.sequenceiq.environment.api.environment.model.response.LocationV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Response;
import com.sequenceiq.environment.environment.converter.network.EnvironmentNetworkConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

@Component
public class EnvironmentViewToSimpleEnvironmentV1ResponseConverter extends
        AbstractConversionServiceAwareConverter<EnvironmentView, SimpleEnvironmentV1Response> {

    @Inject
    private RegionConverter regionConverter;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Override
    public SimpleEnvironmentV1Response convert(EnvironmentView source) {
        SimpleEnvironmentV1Response response = new SimpleEnvironmentV1Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setLocation(getConversionService().convert(source, LocationV1Response.class));
        setNetworkIfPossible(response, source);
        return response;
    }

    private void setNetworkIfPossible(SimpleEnvironmentV1Response response, EnvironmentView source) {
        BaseNetwork network = source.getNetwork();
        if (network != null) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(source.getCloudPlatform());
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(cloudPlatform);
            if (environmentNetworkConverter != null) {
                EnvironmentNetworkV1Response networkV1Response = environmentNetworkConverter.convert(source.getNetwork());
                response.setNetwork(networkV1Response);
            }
        }
    }
}
