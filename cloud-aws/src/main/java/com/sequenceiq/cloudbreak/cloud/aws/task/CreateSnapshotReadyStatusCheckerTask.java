package com.sequenceiq.cloudbreak.cloud.aws.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(CreateSnapshotReadyStatusCheckerTask.NAME)
@Scope("prototype")
public class CreateSnapshotReadyStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "createSnapshotReadyStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateSnapshotReadyStatusCheckerTask.class);

    private final String snapshotId;

    private final AmazonEC2Client ec2Client;

    public CreateSnapshotReadyStatusCheckerTask(AuthenticatedContext authenticatedContext, String snapshotId, AmazonEC2Client ec2Client) {
        super(authenticatedContext, true);
        this.snapshotId = snapshotId;
        this.ec2Client = ec2Client;
    }

    @Override
    protected Boolean doCall() {
        LOGGER.debug("Checking if AWS EBS snapshot '{}' is ready.", snapshotId);
        DescribeSnapshotsResult result = ec2Client.describeSnapshots(new DescribeSnapshotsRequest().withSnapshotIds(snapshotId));
        return result.getSnapshots() != null && !result.getSnapshots().isEmpty() && "completed".equals(result.getSnapshots().get(0).getState());
    }
}
