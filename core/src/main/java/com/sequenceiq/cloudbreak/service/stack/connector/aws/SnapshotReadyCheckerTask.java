package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class SnapshotReadyCheckerTask extends StackBasedStatusCheckerTask<SnapshotReadyContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotReadyCheckerTask.class);

    @Inject
    private AwsStackUtil awsStackUtil;

    @Override
    public boolean checkStatus(SnapshotReadyContext context) {
        LOGGER.info("Checking if AWS EBS snapshot '{}' is ready.", context.getSnapshotId());
        AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(context.getStack());
        DescribeSnapshotsResult result = ec2Client.describeSnapshots(new DescribeSnapshotsRequest().withSnapshotIds(context.getSnapshotId()));
        return result.getSnapshots() != null && !result.getSnapshots().isEmpty() && "completed".equals(result.getSnapshots().get(0).getState());
    }

    @Override
    public void handleTimeout(SnapshotReadyContext context) {
        throw new AwsResourceException(String.format("Timeout while polling AWS EBS snapshot creation. SnapshotID: %s", context.getSnapshotId()));
    }

    @Override
    public String successMessage(SnapshotReadyContext context) {
        return String.format("AWS EBS snapshot created successfully. SnapshotID: %s", context.getSnapshotId());
    }
}