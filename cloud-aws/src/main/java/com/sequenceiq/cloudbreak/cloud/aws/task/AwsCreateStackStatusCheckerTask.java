package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Component(AwsCreateStackStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class AwsCreateStackStatusCheckerTask extends AbstractAwsStackStatusCheckerTask {
    public static final String NAME = "awsCreateStackStatusCheckerTask";

    private final AmazonAutoScalingClient asClient;

    public AwsCreateStackStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cfClient, AmazonAutoScalingClient asClient,
            StackStatus successStatus, StackStatus errorStatus, List<StackStatus> stackErrorStatuses, String cloudFormationStackName) {
        super(authenticatedContext, cfClient, successStatus, errorStatus, stackErrorStatuses, cloudFormationStackName, true);

        this.asClient = asClient;
    }

    @Override
    protected boolean doCheck(Stack cfStack, List<StackEvent> stackEvents) {
        if (!stackEvents.isEmpty()) {
            final List<Activity> failedActivities = new ArrayList<>();
            stackEvents.stream().filter(e -> e.getResourceType().equals("AWS::AutoScaling::AutoScalingGroup") && !e.getPhysicalResourceId().isEmpty())
                    .map(StackEvent::getPhysicalResourceId).forEach(id -> {
                DescribeScalingActivitiesRequest activitiesRequest = new DescribeScalingActivitiesRequest().withAutoScalingGroupName(id);
                Optional<Activity> activity = asClient.describeScalingActivities(activitiesRequest).getActivities().stream()
                        .filter(a -> "Cancelled,Failed".contains(a.getStatusCode())).findFirst();
                if (activity.isPresent()) {
                    failedActivities.add(activity.get());
                }
            });
            if (!failedActivities.isEmpty()) {
                String errors = Joiner.on("], [").join(failedActivities.stream()
                        .map(a -> String.format("%s: %s", a.getAutoScalingGroupName(), a.getStatusMessage())).toArray());
                throw new CloudConnectorException(String.format("AWS AutoScaling groups creation went fail: [%s]", errors));
            }
        }
        return isSuccess(cfStack, stackEvents);
    }

    @Override
    protected boolean handleError(AmazonServiceException e) {
        throw new CloudConnectorException(getErrorMessage(e.getErrorCode(), e.getErrorMessage()));
    }
}