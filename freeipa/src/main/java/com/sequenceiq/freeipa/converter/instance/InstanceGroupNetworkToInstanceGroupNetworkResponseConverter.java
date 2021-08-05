package com.sequenceiq.freeipa.converter.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupNetworkResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.InstanceGroupAwsNetworkParameters;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;

@Component
public class InstanceGroupNetworkToInstanceGroupNetworkResponseConverter implements Converter<InstanceGroupNetwork, InstanceGroupNetworkResponse> {

    @Override
    public InstanceGroupNetworkResponse convert(InstanceGroupNetwork source) {
        InstanceGroupNetworkResponse response = new InstanceGroupNetworkResponse();
        switch (source.cloudPlatform()) {
            case "AWS":
                Json attributes = source.getAttributes();
                Map<String, Object> map = attributes.getMap();
                InstanceGroupAwsNetworkParameters instanceGroupAwsNetworkParameters = new InstanceGroupAwsNetworkParameters();
                instanceGroupAwsNetworkParameters.setSubnetIds((List<String>) map.getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>()));
                response.setAws(instanceGroupAwsNetworkParameters);
                break;
            default:
                response.setAws(null);
        }
        return response;
    }

}
