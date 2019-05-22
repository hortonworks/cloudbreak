package com.sequenceiq.environment.environment.v1.converter;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.network.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.network.BaseNetwork;

@Component
public class EnvironmentViewToSimpleEnvironmentResponseConverter extends
        AbstractConversionServiceAwareConverter<EnvironmentView, SimpleEnvironmentResponse> {

    @Inject
    private RegionConverter regionConverter;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Override
    public SimpleEnvironmentResponse convert(EnvironmentView source) {
        SimpleEnvironmentResponse response = new SimpleEnvironmentResponse();
        response.setId(source.getResourceCrn());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setLocation(getConversionService().convert(source, LocationResponse.class));
        setNetworkIfPossible(response, source);
        return response;
    }

    private void setNetworkIfPossible(SimpleEnvironmentResponse response, EnvironmentView source) {
        BaseNetwork network = source.getNetwork();
        if (network != null) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(source.getCloudPlatform());
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(cloudPlatform);
            if (environmentNetworkConverter != null) {
                EnvironmentNetworkResponse networkV1Response = environmentNetworkConverter.convert(source.getNetwork());
                response.setNetwork(networkV1Response);
            }
        }
    }
}
