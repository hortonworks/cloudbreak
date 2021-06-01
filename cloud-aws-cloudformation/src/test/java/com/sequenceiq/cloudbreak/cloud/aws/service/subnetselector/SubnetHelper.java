package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

public class SubnetHelper {

    public long countDifferentAZs(List<CloudSubnet> chosenSubnets) {
        return chosenSubnets.stream().map(CloudSubnet::getAvailabilityZone).distinct().count();
    }
}
