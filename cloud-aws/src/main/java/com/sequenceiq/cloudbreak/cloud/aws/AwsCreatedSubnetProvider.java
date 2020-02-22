package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;

@Component
class AwsCreatedSubnetProvider {

    Set<CreatedSubnet> provide(Map<String, String> output, int numberOfSubnets, boolean privateSubnetEnabled) {
        Set<CreatedSubnet> subnets = new HashSet<>();
        for (int i = 0; i < numberOfSubnets / 2; i++) {
            CreatedSubnet createdPublicSubnet = new CreatedSubnet();
            createdPublicSubnet.setSubnetId(getValue(output, "PublicSubnetId" + i));
            createdPublicSubnet.setCidr(getValue(output, "PublicSubnetCidr" + i));
            createdPublicSubnet.setAvailabilityZone(getValue(output, "Az" + i));
            createdPublicSubnet.setPublicSubnet(true);
            createdPublicSubnet.setMapPublicIpOnLaunch(true);
            createdPublicSubnet.setIgwAvailable(true);
            subnets.add(createdPublicSubnet);

            if (privateSubnetEnabled) {
                CreatedSubnet createdPrivateSubnet = new CreatedSubnet();
                createdPrivateSubnet.setSubnetId(getValue(output, "PrivateSubnetId" + i));
                createdPrivateSubnet.setCidr(getValue(output, "PrivateSubnetCidr" + i));
                createdPrivateSubnet.setAvailabilityZone(getValue(output, "Az" + i));
                createdPrivateSubnet.setPublicSubnet(false);
                createdPrivateSubnet.setMapPublicIpOnLaunch(false);
                createdPrivateSubnet.setIgwAvailable(false);
                subnets.add(createdPrivateSubnet);
            }
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
