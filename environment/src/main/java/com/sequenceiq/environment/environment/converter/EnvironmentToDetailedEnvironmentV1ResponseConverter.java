package com.sequenceiq.environment.environment.converter;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.response.DetailedEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.EnvironmentNetworkV1Response;
import com.sequenceiq.environment.api.environment.model.response.LocationV1Response;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;
import com.sequenceiq.environment.environment.converter.network.EnvironmentNetworkConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

@Component
public class EnvironmentToDetailedEnvironmentV1ResponseConverter extends AbstractConversionServiceAwareConverter<Environment, DetailedEnvironmentV1Response> {

    @Inject
    private RegionConverter regionConverter;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Override
    public DetailedEnvironmentV1Response convert(Environment source) {
        DetailedEnvironmentV1Response response = new DetailedEnvironmentV1Response();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setRegions(regionConverter.convertRegions(source.getRegionSet()));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCredentialName(source.getCredential().getName());
        response.setLocation(getConversionService().convert(source, LocationV1Response.class));
        response.setProxies(
                source.getProxyConfigs()
                        .stream()
                        .map(proxyConfig -> getConversionService().convert(proxyConfig, ProxyV1Response.class))
                        .collect(Collectors.toSet()));
        setNetworkIfPossible(response, source);
        return response;
    }

    private void setNetworkIfPossible(DetailedEnvironmentV1Response response, Environment source) {
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
