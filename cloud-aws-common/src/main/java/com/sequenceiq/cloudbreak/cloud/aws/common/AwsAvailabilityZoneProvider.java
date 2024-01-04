package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;

@Service
public class AwsAvailabilityZoneProvider {

    @Inject
    private CommonAwsClient awsClient;

    @Cacheable(cacheNames = "cloudResourceAzCache", key = "{ #cloudCredential?.id, #awsRegion.regionName }")
    public List<AvailabilityZone> describeAvailabilityZones(CloudCredential cloudCredential,
            DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest, software.amazon.awssdk.services.ec2.model.Region awsRegion) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), awsRegion.regionName());
        return ec2Client.describeAvailabilityZones(describeAvailabilityZonesRequest).availabilityZones();
    }
}
