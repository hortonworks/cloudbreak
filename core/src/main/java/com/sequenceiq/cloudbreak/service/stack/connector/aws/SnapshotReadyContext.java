package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class SnapshotReadyContext extends StackContext {

    private CreateSnapshotResult snapshotResult;
    private String snapshotId;

    public SnapshotReadyContext(Stack stack, CreateSnapshotResult snapshotResult, String snapshotId) {
        super(stack);
        this.snapshotResult = snapshotResult;
        this.snapshotId = snapshotId;
    }

    public CreateSnapshotResult getSnapshotResult() {
        return snapshotResult;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

}
