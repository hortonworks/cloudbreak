package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;

@Component
class AwsCreatedSubnetProvider {

    Set<CreatedSubnet> provide(Map<String, String> output, List<SubnetRequest> subnetRequests) {
        Set<CreatedSubnet> subnets = new HashSet<>();
        for (SubnetRequest subnetRequest : subnetRequests) {
            CreatedSubnet createdPublicSubnet = new CreatedSubnet();
            createdPublicSubnet.setSubnetId(getValue(output, "id" + subnetRequest.getIndex()));
            createdPublicSubnet.setAvailabilityZone(subnetRequest.getAvailabilityZone());
            if (!Strings.isNullOrEmpty(subnetRequest.getPrivateSubnetCidr())) {
                createdPublicSubnet.setCidr(subnetRequest.getPrivateSubnetCidr());
                createdPublicSubnet.setPublicSubnet(false);
                createdPublicSubnet.setMapPublicIpOnLaunch(false);
                createdPublicSubnet.setIgwAvailable(false);
            } else {
                createdPublicSubnet.setCidr(subnetRequest.getPublicSubnetCidr());
                createdPublicSubnet.setPublicSubnet(true);
                createdPublicSubnet.setMapPublicIpOnLaunch(true);
                createdPublicSubnet.setIgwAvailable(true);
            }
            subnets.add(createdPublicSubnet);
        }
        return subnets;
    }

    private String getValue(Map<String, String> output, String key) {
        if (output.containsKey(key)) {
            return output.get(key);
        } else {
            throw new CloudConnectorException(String.format("%s could not be found in the CloudFormation stack output.", key));
        }
    }
}
