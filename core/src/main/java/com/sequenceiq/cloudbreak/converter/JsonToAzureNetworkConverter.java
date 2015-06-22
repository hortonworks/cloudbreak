package com.sequenceiq.cloudbreak.converter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.controller.validation.AzureNetworkParam;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;

@Component
public class JsonToAzureNetworkConverter extends AbstractConversionServiceAwareConverter<NetworkJson, AzureNetwork> {

    @Override
    public AzureNetwork convert(NetworkJson source) {
        AzureNetwork network = new AzureNetwork();
        network.setName(source.getName());
        network.setDescription(source.getDescription());
        network.setSubnetCIDR(source.getSubnetCIDR());
        network.setPublicInAccount(source.isPublicInAccount());
        network.setStatus(ResourceStatus.USER_MANAGED);
        Map<String, Object> parameters = source.getParameters();
        if (parameters.containsKey(AzureNetworkParam.ADDRESS_PREFIX_CIDR.getName())) {
            String addressPrefix = (String) parameters.get(AzureNetworkParam.ADDRESS_PREFIX_CIDR.getName());
            network.setAddressPrefixCIDR(addressPrefix);
        } else {
            throw new BadRequestException(String.format("The parameters map must contain the '%s' parameter!", AzureNetworkParam.ADDRESS_PREFIX_CIDR.getName()));
        }
        return network;
    }
}
