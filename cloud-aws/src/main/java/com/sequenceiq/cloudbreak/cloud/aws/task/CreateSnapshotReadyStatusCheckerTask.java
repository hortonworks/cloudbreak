package com.sequenceiq.cloudbreak.cloud.aws.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Scope(value = "prototype")
public class CreateSnapshotReadyStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "createSnapshotReadyStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateSnapshotReadyStatusCheckerTask.class);

    private AuthenticatedContext authenticatedContext;
    private CreateSnapshotResult snapshotResult;
    private String snapshotId;
    private AmazonEC2Client ec2Client;

    public CreateSnapshotReadyStatusCheckerTask(AuthenticatedContext authenticatedContext, CreateSnapshotResult snapshotResult, String snapshotId,
            AmazonEC2Client ec2Client) {
        super(authenticatedContext, true);
        this.snapshotId = snapshotId;
        this.snapshotResult = snapshotResult;
        this.ec2Client = ec2Client;
    }

    @Override
    public Boolean call() {
        LOGGER.info("Checking if AWS EBS snapshot '{}' is ready.", snapshotId);
        DescribeSnapshotsResult result = ec2Client.describeSnapshots(new DescribeSnapshotsRequest().withSnapshotIds(snapshotId));
        return result.getSnapshots() != null && !result.getSnapshots().isEmpty() && "completed".equals(result.getSnapshots().get(0).getState());
    }
}
