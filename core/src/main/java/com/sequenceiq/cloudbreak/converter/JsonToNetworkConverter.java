package com.sequenceiq.cloudbreak.converter;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class JsonToNetworkConverter extends AbstractConversionServiceAwareConverter<NetworkRequest, Network> {
    @Inject
    private TopologyService topologyService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public Network convert(NetworkRequest source) {
        Network network = new Network();
        if (Strings.isNullOrEmpty(source.getName())) {
            network.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        } else {
            network.setName(source.getName());
        }
        network.setDescription(source.getDescription());
        network.setSubnetCIDR(source.getSubnetCIDR());
        network.setStatus(ResourceStatus.USER_MANAGED);
        network.setCloudPlatform(source.getCloudPlatform());
        Map<String, Object> parameters = source.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            try {
                network.setAttributes(new Json(parameters));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        if (source.getTopologyId() != null) {
            network.setTopology(topologyService.getById(source.getTopologyId()));
        }
        return network;
    }
}
