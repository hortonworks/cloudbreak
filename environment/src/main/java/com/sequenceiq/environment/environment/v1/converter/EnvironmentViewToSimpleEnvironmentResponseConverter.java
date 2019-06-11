package com.sequenceiq.environment.environment.v1.converter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.network.domain.BaseNetwork;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Component
public class EnvironmentViewToSimpleEnvironmentResponseConverter extends
        AbstractConversionServiceAwareConverter<EnvironmentView, SimpleEnvironmentResponse> {

    private final RegionConverter regionConverter;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    public EnvironmentViewToSimpleEnvironmentResponseConverter(RegionConverter regionConverter,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap, CredentialToCredentialV1ResponseConverter credentialConverter) {
        this.regionConverter = regionConverter;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.credentialConverter = credentialConverter;
    }

    @Override
    public SimpleEnvironmentResponse convert(EnvironmentView source) {
        SimpleEnvironmentResponse response = new SimpleEnvironmentResponse();
        response.setCrn(source.getResourceCrn());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredential(credentialConverter.convert(source.getCredential()));
        response.setLocation(getConversionService().convert(source, LocationResponse.class));
        response.setEnvironmentStatus(source.getStatus().getResponseStatus());
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
