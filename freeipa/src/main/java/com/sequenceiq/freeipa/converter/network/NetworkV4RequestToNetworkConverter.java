package com.sequenceiq.freeipa.converter.network;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.json.Json;
import com.sequenceiq.freeipa.service.MissingResourceNameGenerator;

@Component
public class NetworkV4RequestToNetworkConverter implements Converter<NetworkV4Request, Network> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public Network convert(NetworkV4Request source) {
        Network network = new Network();
        network.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));

        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        if (parameters != null) {
            try {
                network.setAttributes(new Json(parameters));
            } catch (JsonProcessingException ignored) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        return network;
    }
}
