package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.controller.validation.OpenStackNetworkParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackNetwork;

@Component
public class OpenStackNetworkToJsonConverter extends AbstractConversionServiceAwareConverter<OpenStackNetwork, NetworkJson> {

    @Override
    public NetworkJson convert(OpenStackNetwork source) {
        NetworkJson json = new NetworkJson();
        json.setId(source.getId().toString());
        json.setCloudPlatform(CloudPlatform.OPENSTACK);
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setSubnetCIDR(source.getSubnetCIDR());
        json.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackNetworkParam.PUBLIC_NET_ID.getName(), source.getPublicNetId());
        json.setParameters(parameters);
        return json;
    }
}
