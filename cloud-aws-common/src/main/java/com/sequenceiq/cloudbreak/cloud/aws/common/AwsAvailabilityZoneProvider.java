package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.List;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsAvailabilityZoneProvider {

    @Inject
    private CommonAwsClient awsClient;

    @Cacheable(cacheNames = "cloudResourceAzCache", key = "{ #cloudCredential?.id, #awsRegion.regionName }")
    public List<AvailabilityZone> describeAvailabilityZones(CloudCredential cloudCredential,
            DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest, com.amazonaws.services.ec2.model.Region awsRegion) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), awsRegion.getRegionName());
        DescribeAvailabilityZonesResult describeAvailabilityZonesResult = ec2Client.describeAvailabilityZones(describeAvailabilityZonesRequest);
        return describeAvailabilityZonesResult.getAvailabilityZones();
    }
}
