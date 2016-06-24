package com.sequenceiq.cloudbreak.converter;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class JsonToNetworkConverter extends AbstractConversionServiceAwareConverter<NetworkJson, Network> {
    @Inject
    private TopologyService topologyService;

    @Override
    public Network convert(NetworkJson source) {
        Network network = new Network();
        network.setName(source.getName());
        network.setDescription(source.getDescription());
        network.setSubnetCIDR(source.getSubnetCIDR());
        network.setPublicInAccount(source.isPublicInAccount());
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
            network.setTopology(topologyService.get(source.getTopologyId()));
        }
        return network;
    }
}
