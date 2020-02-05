package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;

@Component
public class AwsSubnetRequestProvider {

    public List<SubnetRequest> provide(AmazonEC2Client ec2Client, List<String> subnetCidrs) {
        Assert.isTrue(subnetCidrs.size() % 2 == 0, "The number of the subnets should be even!");
        List<String> az = getAvailabilityZones(ec2Client);
        List<SubnetRequest> subnets = new ArrayList<>();
        int sunetIndex = 0;
        for (int i = 0; i < subnetCidrs.size() / 2; i++) {
            SubnetRequest subnetRequest = new SubnetRequest();
            subnetRequest.setPublicSubnetCidr(subnetCidrs.get(sunetIndex));
            subnetRequest.setPrivateSubnetCidr(subnetCidrs.get(sunetIndex + 1));
            sunetIndex += 2;
            if (i < az.size()) {
                subnetRequest.setAvailabilityZone(az.get(i));
            } else {
                subnetRequest.setAvailabilityZone(az.get(az.size() - 1));
            }
            subnets.add(subnetRequest);
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
