package com.sequenceiq.freeipa.converter.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupNetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.InstanceGroupAwsNetworkParameters;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;

@Component
public class InstanceGroupNetworkRequestToInstanceGroupNetworkConverter {

    public InstanceGroupNetwork convert(String cloudPlatform, @Nonnull InstanceGroupNetworkRequest source) {
        InstanceGroupNetwork entity = new InstanceGroupNetwork();
        entity.setCloudPlatform(cloudPlatform);
        Map<String, Object> params = new HashMap<>();
        InstanceGroupAwsNetworkParameters aws = source.getAws();
        if (aws != null && aws.getSubnetIds() != null) {
            List<String> subnetIds = aws.getSubnetIds();
            params.put(NetworkConstants.SUBNET_IDS, subnetIds);
        }
        entity.setAttributes(new Json(params));
        return entity;
    }
}
