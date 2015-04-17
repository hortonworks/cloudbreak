package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.controller.validation.AwsNetworkParam;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AwsNetworkToJsonConverter extends AbstractConversionServiceAwareConverter<AwsNetwork, NetworkJson> {

    @Override
    public NetworkJson convert(AwsNetwork source) {
        NetworkJson json = new NetworkJson();
        json.setId(source.getId().toString());
        json.setCloudPlatform(CloudPlatform.AWS);
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setSubnetCIDR(source.getSubnetCIDR());
        json.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsNetworkParam.VPC_ID.getName(), source.getVpcId());
        parameters.put(AwsNetworkParam.INTERNET_GATEWAY_ID.getName(), source.getInternetGatewayId());
        json.setParameters(parameters);
        return json;
    }
}
