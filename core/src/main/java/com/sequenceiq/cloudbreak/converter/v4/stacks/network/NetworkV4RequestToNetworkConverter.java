package com.sequenceiq.cloudbreak.converter.v4.stacks.network;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class NetworkV4RequestToNetworkConverter extends AbstractConversionServiceAwareConverter<NetworkV4Request, Network> {
    @Inject
    private TopologyService topologyService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public Network convert(NetworkV4Request source) {
        Network network = new Network();
        network.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        network.setSubnetCIDR(source.getSubnetCIDR());

        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        if (!parameters.isEmpty()) {
            parameters.put("cloudPlatform", source.getCloudPlatform().name());
            try {
                network.setAttributes(new Json(parameters));
            } catch (JsonProcessingException ignored) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        return network;
    }
}
