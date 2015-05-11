package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpNetwork;

@Component
public class GcpNetworkToJsonConverter extends AbstractConversionServiceAwareConverter<GcpNetwork, NetworkJson> {

    @Override
    public NetworkJson convert(GcpNetwork source) {
        NetworkJson json = new NetworkJson();
        json.setId(source.getId().toString());
        json.setCloudPlatform(CloudPlatform.GCC);
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setSubnetCIDR(source.getSubnetCIDR());
        json.setPublicInAccount(source.isPublicInAccount());
        return json;
    }
}
