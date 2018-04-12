package com.sequenceiq.cloudbreak.converter;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.json.Json;
import org.springframework.stereotype.Component;

@Component
public class NetworkToNetworkResponseConverter extends AbstractConversionServiceAwareConverter<Network, NetworkResponse> {

    @Override
    public NetworkResponse convert(Network source) {
        NetworkResponse json = new NetworkResponse();
        json.setId(source.getId());
        json.setCloudPlatform(source.cloudPlatform());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setSubnetCIDR(source.getSubnetCIDR());
        json.setPublicInAccount(source.isPublicInAccount());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            json.setParameters(Maps.newHashMap(attributes.getMap()));
        }
        if (source.getTopology() != null) {
            json.setTopologyId(source.getTopology().getId());
        }
        return json;
    }
}
