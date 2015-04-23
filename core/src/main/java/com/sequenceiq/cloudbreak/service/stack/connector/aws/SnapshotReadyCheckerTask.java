package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class SnapshotReadyCheckerTask extends StackBasedStatusCheckerTask<SnapshotReadyPollerObject> {

    @Override
    public boolean checkStatus(SnapshotReadyPollerObject object) {
        DescribeSnapshotsResult result = object.getClient().describeSnapshots(new DescribeSnapshotsRequest().withSnapshotIds(object.getSnapshotId()));
        if (result.getSnapshots() != null && !result.getSnapshots().isEmpty()) {
            return "completed".equals(result.getSnapshots().get(0).getState()) ? true : false;
        }
        return false;
    }

    @Override
    public void handleTimeout(SnapshotReadyPollerObject object) {
        throw new CloudFormationStackException(String.format(
                "AWS Ebs disk snapshot creation didn't reach the desired state in the given time frame, stack id: %s, name %s",
                object.getStack().getId(), object.getStack().getName()));
    }

    @Override
    public String successMessage(SnapshotReadyPollerObject object) {
        return String.format("AWS Ebs disk snapshot creation success on  stack(%s)", object.getStack().getId());
    }
}