package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class AwsPollTaskFactory {
    @Inject
    private ApplicationContext applicationContext;

    public PollTask<Boolean> newAwsCloudformationStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient client,
            StackStatus successStatus, StackStatus errorStatus, List<StackStatus> stackErrorStatuses, String cloudFormationStackName, boolean cancellable) {
        return createPollTask(AwsCloudformationStatusCheckerTask.NAME, authenticatedContext, client, successStatus, errorStatus,
                stackErrorStatuses, cloudFormationStackName, cancellable);
    }

    public PollTask<Boolean> newEbsVolumeStatusCheckerTask(AuthenticatedContext authenticatedContext, CloudStack stack,
            AmazonEC2Client amazonEC2Client, String volumeId) {
        return createPollTask(EbsVolumeStatusCheckerTask.NAME, authenticatedContext, stack, amazonEC2Client, volumeId);
    }

    public PollTask<Boolean> newCreateSnapshotReadyStatusCheckerTask(AuthenticatedContext authenticatedContext, CreateSnapshotResult snapshotResult,
            String snapshotId, AmazonEC2Client ec2Client) {
        return createPollTask(CreateSnapshotReadyStatusCheckerTask.NAME, authenticatedContext, snapshotResult, snapshotId, ec2Client);
    }

    public PollTask<Boolean> newASGroupStatusCheckerTask(AuthenticatedContext authenticatedContext, String asGroupName, Integer requiredInstances,
            AwsClient awsClient, CloudFormationStackUtil cloudFormationStackUtil) {
        return createPollTask(ASGroupStatusCheckerTask.NAME, authenticatedContext, asGroupName, requiredInstances, awsClient, cloudFormationStackUtil);
    }

    @SuppressWarnings("unchecked")
    private <T> T createPollTask(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }
}
