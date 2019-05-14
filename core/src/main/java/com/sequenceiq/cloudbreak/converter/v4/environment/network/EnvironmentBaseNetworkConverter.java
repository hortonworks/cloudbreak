package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.EnvironmentNetworkV4Response;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.RegistrationType;
import com.sequenceiq.cloudbreak.domain.environment.Subnet;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBaseNetworkConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private SubnetConverter subnetConverter;

    @Override
    public BaseNetwork convert(EnvironmentNetworkV4Request source, Environment environment) {
        BaseNetwork result = createProviderSpecificNetwork(source);
        setBasicProperties(source, environment, result, RegistrationType.EXISTING);
        result.setSubnets(source.getSubnetIds().stream().map(Subnet::new).collect(Collectors.toSet()));
        return result;
    }

    @Override
    public BaseNetwork convertNewNetwork(EnvironmentNetworkV4Request source, Environment environment, CreatedCloudNetwork createdCloudNetwork) {
        BaseNetwork result = createProviderSpecificNetwork(source, createdCloudNetwork);
        setBasicProperties(source, environment, result, RegistrationType.CREATE_NEW);
        result.setSubnets(subnetConverter.convert(createdCloudNetwork.getSubnets()));
        return result;
    }

    private void setBasicProperties(EnvironmentNetworkV4Request source, Environment environment, BaseNetwork result, RegistrationType existing) {
        result.setName(environment.getName());
        result.setEnvironment(environment);
        result.setWorkspace(environment.getWorkspace());
        result.setNetworkCidr(source.getNetworkCidr());
        result.setRegistrationType(existing);
    }

    @Override
    public EnvironmentNetworkV4Response convert(BaseNetwork source) {
        EnvironmentNetworkV4Response result = new EnvironmentNetworkV4Response();
        result.setId(source.getId());
        result.setName(source.getName());
        result.setSubnetIds(getSubnetIds(source));
        result = setProviderSpecificFields(result, source);
        return result;
    }

    @Override
    public Network convertToLegacyNetwork(BaseNetwork source) {
        Network result = new Network();
        result.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        result.setSubnetCIDR(null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("subnetId", String.join(",", getSubnetIds(source)));
        attributes.put("subnetCidrs", String.join(",", getSubnetCidrs(source)));
        attributes.put("cloudPlatform", getCloudPlatform().name());
        attributes.putAll(getAttributesForLegacyNetwork(source));
        result.setAttributes(new Json(attributes));
        return result;
    }

    private Set<String> getSubnetIds(BaseNetwork source) {
        return source.getSubnetSet().stream().map(Subnet::getSubnetId).collect(Collectors.toSet());
    }

    private Set<String> getSubnetCidrs(BaseNetwork source) {
        return source.getSubnetSet().stream().map(Subnet::getCidr).collect(Collectors.toSet());
    }

    abstract BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV4Request source, CreatedCloudNetwork createdCloudNetwork);

    abstract BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV4Request source);

    abstract EnvironmentNetworkV4Response setProviderSpecificFields(EnvironmentNetworkV4Response result, BaseNetwork source);

    abstract Map<String, Object> getAttributesForLegacyNetwork(BaseNetwork source);
}
