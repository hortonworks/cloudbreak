package com.sequenceiq.freeipa.converter.network;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.entity.Network;

@Component
public class NetworkRequestToNetworkConverter implements Converter<NetworkRequest, Network> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public Network convert(NetworkRequest source) {
        Network network = new Network();
        network.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        network.setOutboundInternetTraffic(getOutboundInternetTraffic(source));
        network.setNetworkCidrs(source.getNetworkCidrs());
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

    private OutboundInternetTraffic getOutboundInternetTraffic(NetworkRequest network) {
        return Optional.ofNullable(network.getOutboundInternetTraffic()).orElse(OutboundInternetTraffic.ENABLED);
    }
}
