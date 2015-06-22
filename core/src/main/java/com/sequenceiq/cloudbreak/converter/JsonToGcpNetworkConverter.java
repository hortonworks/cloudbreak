package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.GcpNetwork;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;

@Component
public class JsonToGcpNetworkConverter extends AbstractConversionServiceAwareConverter<NetworkJson, GcpNetwork> {

    @Override
    public GcpNetwork convert(NetworkJson source) {
        GcpNetwork network = new GcpNetwork();
        network.setName(source.getName());
        network.setDescription(source.getDescription());
        network.setSubnetCIDR(source.getSubnetCIDR());
        network.setPublicInAccount(source.isPublicInAccount());
        network.setStatus(ResourceStatus.USER_MANAGED);
        return network;
    }
}
