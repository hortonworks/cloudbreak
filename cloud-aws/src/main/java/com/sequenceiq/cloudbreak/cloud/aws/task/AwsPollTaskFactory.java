package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class AwsPollTaskFactory {
    @Inject
    private ApplicationContext applicationContext;

    public PollTask<Boolean> newAwsCreateNetworkStatusCheckerTask(AmazonCloudFormationClient cfClient, StackStatus successStatus, StackStatus errorStatus,
            List<StackStatus> stackErrorStatuses, String cloudFormationStackName) {
        return createPollTask(AwsCreateNetworkStatusCheckerTask.NAME, cfClient, successStatus, errorStatus, stackErrorStatuses, cloudFormationStackName);
    }

    public PollTask<Boolean> newAwsCreateStackStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cfClient,
            AmazonAutoScalingClient asClient, StackStatus successStatus, StackStatus errorStatus, List<StackStatus> stackErrorStatuses,
            String cloudFormationStackName) {
        return createPollTask(AwsCreateStackStatusCheckerTask.NAME, authenticatedContext, cfClient, asClient, successStatus, errorStatus,
                stackErrorStatuses, cloudFormationStackName);
    }

    public PollTask<Boolean> newAwsTerminateStackStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cfClient,
            StackStatus successStatus, StackStatus errorStatus, List<StackStatus> stackErrorStatuses, String cloudFormationStackName) {
        return createPollTask(AwsTerminateStackStatusCheckerTask.NAME, authenticatedContext, cfClient, successStatus, errorStatus,
                stackErrorStatuses, cloudFormationStackName);
    }

    public PollTask<Boolean> newASGroupStatusCheckerTask(AuthenticatedContext authenticatedContext, String asGroupName, Integer requiredInstances,
            AwsClient awsClient, CloudFormationStackUtil cloudFormationStackUtil) {
        return createPollTask(ASGroupStatusCheckerTask.NAME, authenticatedContext, asGroupName, requiredInstances, awsClient, cloudFormationStackUtil);
    }

    public PollTask<Boolean> newEbsVolumeStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonEC2Client amazonEC2Client, String volumeId) {
        return createPollTask(EbsVolumeStatusCheckerTask.NAME, authenticatedContext, amazonEC2Client, volumeId);
    }

    public PollTask<Boolean> newCreateSnapshotReadyStatusCheckerTask(AuthenticatedContext authenticatedContext, String snapshotId, AmazonEC2Client ec2Client) {
        return createPollTask(CreateSnapshotReadyStatusCheckerTask.NAME, authenticatedContext, snapshotId, ec2Client);
    }

    public PollTask<Boolean> newInstanceTerminationStatusCheckerTask(AuthenticatedContext authenticatedContext, String instanceId, AmazonEC2Client ec2Client) {
        return createPollTask(AwsInstanceTerminatedStatusCheckerTask.NAME, authenticatedContext, instanceId, ec2Client);
    }

    public PollTask<Boolean> newAMICopyStatusCheckerTask(AuthenticatedContext authenticatedContext, Collection<String> imageIds, AmazonEC2Client client) {
        return createPollTask(AMICopyStatusCheckerTask.NAME, authenticatedContext, imageIds, client);
    }

    @SuppressWarnings("unchecked")
    private <T> T createPollTask(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }
}
