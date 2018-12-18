package com.sequenceiq.cloudbreak.converter.v2;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class NetworkV2RequestToNetworkConverter extends AbstractConversionServiceAwareConverter<NetworkV2Request, Network> {

    @Override
    public Network convert(NetworkV2Request source) {
        Network network = new Network();
        network.setSubnetCIDR(source.getSubnetCIDR());
        if (source.getParameters() != null && !source.getParameters().isEmpty()) {
            try {
                network.setAttributes(new Json(source.getParameters()));
            } catch (JsonProcessingException ignored) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        return network;
    }
}
