package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.controller.validation.AzureNetworkParam;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AzureNetworkToJsonConverter extends AbstractConversionServiceAwareConverter<AzureNetwork, NetworkJson> {

    @Override
    public NetworkJson convert(AzureNetwork source) {
        NetworkJson json = new NetworkJson();
        json.setId(source.getId().toString());
        json.setCloudPlatform(CloudPlatform.AZURE);
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setSubnetCIDR(source.getSubnetCIDR());
        json.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AzureNetworkParam.ADDRESS_PREFIX_CIDR.getName(), source.getAddressPrefixCIDR());
        json.setParameters(parameters);
        return json;
    }
}
