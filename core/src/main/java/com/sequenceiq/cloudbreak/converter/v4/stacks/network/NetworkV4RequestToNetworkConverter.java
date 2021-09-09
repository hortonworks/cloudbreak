package com.sequenceiq.cloudbreak.converter.v4.stacks.network;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@Component
public class NetworkV4RequestToNetworkConverter {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    public Network convert(NetworkV4Request source) {
        Network network = new Network();
        network.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        network.setSubnetCIDR(source.getSubnetCIDR());
        network.setOutboundInternetTraffic(OutboundInternetTraffic.ENABLED);

        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        if (parameters != null) {
            try {
                network.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException ignored) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        return network;
    }
}
