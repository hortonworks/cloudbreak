package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonEc2RetryClient extends AmazonRetryClient {

    private final AmazonEC2Client client;

    private final Retry retry;

    public AmazonEc2RetryClient(AmazonEC2Client client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public CreateVolumeResult createVolume(CreateVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.createVolume(request)));
    }

    public DescribeSubnetsResult describeSubnets(DescribeSubnetsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeSubnets(request)));
    }

    public ModifyInstanceAttributeResult modifyInstanceAttribute(ModifyInstanceAttributeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.modifyInstanceAttribute(request)));
    }

    public DeleteVolumeResult deleteVolume(DeleteVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.deleteVolume(request)));
    }

    public DescribeVolumesResult describeVolumes(DescribeVolumesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeVolumes(request)));
    }

    public AttachVolumeResult attachVolume(AttachVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.attachVolume(request)));
    }
}
