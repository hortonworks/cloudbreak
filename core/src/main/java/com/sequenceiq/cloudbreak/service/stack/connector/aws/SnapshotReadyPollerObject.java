package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class SnapshotReadyPollerObject extends StackContext {

    private CreateSnapshotResult snapshotResult;
    private String snapshotId;
    private AmazonEC2Client client;


    public SnapshotReadyPollerObject(Stack stack, CreateSnapshotResult snapshotResult, String snapshotId, AmazonEC2Client client) {
        super(stack);
        this.snapshotResult = snapshotResult;
        this.snapshotId = snapshotId;
        this.client = client;
    }

    public CreateSnapshotResult getSnapshotResult() {
        return snapshotResult;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public AmazonEC2Client getClient() {
        return client;
    }
}
