package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class EbsVolumeStateCheckerTask extends StackBasedStatusCheckerTask<EbsVolumeContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EbsVolumeStateCheckerTask.class);

    @Inject
    private AwsStackUtil awsStackUtil;

    @Override
    public boolean checkStatus(EbsVolumeContext context) {
        LOGGER.info("Checking if AWS EBS volume '{}' is created.", context.getVolumeId());
        AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(context.getStack());
        DescribeVolumesResult describeVolumesResult = ec2Client.describeVolumes(new DescribeVolumesRequest().withVolumeIds(context.getVolumeId()));
        return describeVolumesResult.getVolumes() != null && !describeVolumesResult.getVolumes().isEmpty()
                && "available".equals(describeVolumesResult.getVolumes().get(0).getState());
    }

    @Override
    public void handleTimeout(EbsVolumeContext ebsVolumeContext) {
        throw new AwsResourceException(String.format("Timeout while polling AWS EBS volume creation. Volume ID: %s", ebsVolumeContext.getVolumeId()));
    }

    @Override
    public String successMessage(EbsVolumeContext ebsVolumeContext) {
        return String.format("AWS EBS Volume created successfully. Volume ID: %s", ebsVolumeContext.getVolumeId());
    }
}