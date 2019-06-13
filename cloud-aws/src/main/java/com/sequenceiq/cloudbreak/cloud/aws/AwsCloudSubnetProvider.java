package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;

@Component
public class AwsCloudSubnetProvider {

    public List<CreatedSubnet> provide(AmazonEC2Client ec2Client, List<String> subnetCidrs) {
        List<String> az = getAvailabilityZones(ec2Client);
        List<CreatedSubnet> subnets = new ArrayList<>(subnetCidrs.size());
        for (int i = 0; i < subnetCidrs.size(); i++) {
            CreatedSubnet createdSubnet = new CreatedSubnet();
            createdSubnet.setCidr(subnetCidrs.get(i));
            if (i < az.size()) {
                createdSubnet.setAvailabilityZone(az.get(i));
            } else {
                createdSubnet.setAvailabilityZone(az.get(az.size() - 1));
            }
            subnets.add(createdSubnet);
        }
        return subnets;
    }

    private List<String> getAvailabilityZones(AmazonEC2Client ec2Client) {
        return ec2Client.describeAvailabilityZones()
                        .getAvailabilityZones()
                        .stream()
                        .map(AvailabilityZone::getZoneName)
                        .collect(Collectors.toList());
    }
}
