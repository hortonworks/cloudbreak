package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class NetworkToJsonConverter extends AbstractConversionServiceAwareConverter<Network, NetworkJson> {

    @Override
    public NetworkJson convert(Network source) {
        NetworkJson json = new NetworkJson();
        json.setId(source.getId().toString());
        json.setCloudPlatform(source.cloudPlatform());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setSubnetCIDR(source.getSubnetCIDR());
        json.setPublicInAccount(source.isPublicInAccount());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            json.setParameters(attributes.getMap());
        }
        return json;
    }
}
