package com.sequenceiq.redbeams.service.network;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.redbeams.exception.BadRequestException;

@Component
public class AwsSubnetValidator {

    public void validate(Collection<CloudSubnet> cloudSubnets, int minSubnetNeededInDifferentAz) {
        validateEnoughSubnetIsPresent(cloudSubnets, minSubnetNeededInDifferentAz);
        validateAzNumber(cloudSubnets, minSubnetNeededInDifferentAz);
    }

    private void validateAzNumber(Collection<CloudSubnet> cloudSubnets, int minSubnetNeededInDifferentAz) {
        long numAZs = cloudSubnets.stream().map(CloudSubnet::getAvailabilityZone).distinct().count();
        if (numAZs < minSubnetNeededInDifferentAz) {
            String message = String.format("Subnets are in %d different AZ, but subnets in %d different AZs required.", numAZs, minSubnetNeededInDifferentAz);
            throw new BadRequestException(message);
        }
    }

    private void validateEnoughSubnetIsPresent(Collection<CloudSubnet> cloudSubnets, int minSubnetNeededInDifferentAz) {
        if (cloudSubnets.size() < minSubnetNeededInDifferentAz) {
            String message = String.format("Insufficient number of subnets: at least %d subnets required", minSubnetNeededInDifferentAz);
            throw new BadRequestException(message);
        }
    }
}
