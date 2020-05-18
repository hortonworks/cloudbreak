package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsAvailabilityZoneProvider {

    @Cacheable(cacheNames = "cloudResourceAzCache", key = "{ #cloudCredential?.id, #awsRegion.regionName }")
    public List<AvailabilityZone> describeAvailabilityZones(CloudCredential cloudCredential,
            DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest, AmazonEC2Client ec2Client,
            com.amazonaws.services.ec2.model.Region awsRegion) {
        DescribeAvailabilityZonesResult describeAvailabilityZonesResult = ec2Client.describeAvailabilityZones(describeAvailabilityZonesRequest);
        return describeAvailabilityZonesResult.getAvailabilityZones();
    }
}
