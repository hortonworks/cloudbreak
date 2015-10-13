package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(ASGroupStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ASGroupStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "aSGroupStatusCheckerTask";

    private static final int INSTANCE_RUNNING = 16;
    private static final int COMPLETED = 100;
    private static final String CANCELLED = "Cancelled";
    private static final Logger LOGGER = LoggerFactory.getLogger(ASGroupStatusCheckerTask.class);

    private String autoScalingGroupName;
    private Integer requiredInstances;
    private AwsClient awsClient;
    private CloudFormationStackUtil cloudFormationStackUtil;

    public ASGroupStatusCheckerTask(AuthenticatedContext authenticatedContext, String asGroupName, Integer requiredInstances, AwsClient awsClient,
            CloudFormationStackUtil cloudFormationStackUtil) {
        super(authenticatedContext, true);
        this.autoScalingGroupName = asGroupName;
        this.requiredInstances = requiredInstances;
        this.awsClient = awsClient;
        this.cloudFormationStackUtil = cloudFormationStackUtil;
    }

    @Override
    public Boolean call() {
        LOGGER.info("Checking status of Auto Scaling group '{}'", autoScalingGroupName);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(getAuthenticatedContext().getCloudCredential()),
                getAuthenticatedContext().getCloudContext().getRegion());
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(getAuthenticatedContext().getCloudCredential()),
                getAuthenticatedContext().getCloudContext().getRegion());
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(autoScalingGroupName, amazonASClient);
        if (instanceIds.size() < requiredInstances) {
            LOGGER.debug("Instances in AS group: {}, needed: {}", instanceIds.size(), requiredInstances);
            DescribeScalingActivitiesRequest describeScalingActivitiesRequest =
                    new DescribeScalingActivitiesRequest().withAutoScalingGroupName(autoScalingGroupName);
            List<Activity> activities = amazonASClient.describeScalingActivities(describeScalingActivitiesRequest).getActivities();
            for (Activity activity : activities) {
                if (activity.getProgress().equals(COMPLETED) && CANCELLED.equals(activity.getStatusCode())) {
                    throw new CancellationException(activity.getStatusMessage());
                }
            }
            return false;
        }
        DescribeInstanceStatusResult describeResult = amazonEC2Client.describeInstanceStatus(new DescribeInstanceStatusRequest().withInstanceIds(instanceIds));
        if (describeResult.getInstanceStatuses().size() < requiredInstances) {
            LOGGER.debug("Instances up: {}, needed: {}", describeResult.getInstanceStatuses().size(), requiredInstances);
            return false;
        }
        for (InstanceStatus status : describeResult.getInstanceStatuses()) {
            if (INSTANCE_RUNNING != status.getInstanceState().getCode()) {
                LOGGER.debug("Instances are up but not all of them are in running state.");
                return false;
            }
        }
        return true;
    }

}
