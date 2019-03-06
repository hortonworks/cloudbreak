package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(ASGroupStatusCheckerTask.NAME)
@Scope("prototype")
public class ASGroupStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "aSGroupStatusCheckerTask";

    private static final int MAX_INSTANCE_ID_SIZE = 100;

    private static final int INSTANCE_RUNNING = 16;

    private static final int COMPLETED = 100;

    private static final String CANCELLED = "Cancelled";

    private static final String FAILED = "Failed";

    private static final String WAIT_FOR_SPOT_INSTANCES_STATUS_CODE = "WaitingForSpotInstanceId";

    private static final String SPOT_ID_PATTERN = "sir-[a-z0-9]{8}";

    private static final String LOW_SPOT_PRICE_STATUS_CODE = "price-too-low";

    private static final Logger LOGGER = LoggerFactory.getLogger(ASGroupStatusCheckerTask.class);

    private final String autoScalingGroupName;

    private final Integer requiredInstances;

    private final AwsClient awsClient;

    private final CloudFormationStackUtil cloudFormationStackUtil;

    private final AmazonAutoScalingRetryClient autoScalingClient;

    private Optional<Activity> latestActivity;

    public ASGroupStatusCheckerTask(AuthenticatedContext authenticatedContext, String asGroupName, Integer requiredInstances, AwsClient awsClient,
            CloudFormationStackUtil cloudFormationStackUtil) {
        super(authenticatedContext, true);
        autoScalingGroupName = asGroupName;
        this.requiredInstances = requiredInstances;
        this.awsClient = awsClient;
        this.cloudFormationStackUtil = cloudFormationStackUtil;
        autoScalingClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(getAuthenticatedContext().getCloudCredential()),
                getAuthenticatedContext().getCloudContext().getLocation().getRegion().value());
        List<Activity> autoScalingActivities = getAutoScalingActivities();
        latestActivity = autoScalingActivities.stream().findFirst();
    }

    @Override
    protected Boolean doCall() {
        LOGGER.debug("Checking status of Auto Scaling group '{}'", autoScalingGroupName);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(getAuthenticatedContext().getCloudCredential()),
                getAuthenticatedContext().getCloudContext().getLocation().getRegion().value());
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(autoScalingClient, autoScalingGroupName);
        if (instanceIds.size() < requiredInstances) {
            LOGGER.debug("Instances in AS group: {}, needed: {}", instanceIds.size(), requiredInstances);
            List<Activity> activities = getAutoScalingActivities();
            if (latestActivity.isPresent()) {
                checkForSpotRequest(latestActivity.get(), amazonEC2Client);
                activities = activities.stream().filter(activity -> activity.getStartTime().after(latestActivity.get().getStartTime()))
                        .collect(Collectors.toList());
            }
            updateLatestActivity(activities);
            checkFailedActivities(activities);
            return false;
        }
        Collection<DescribeInstanceStatusResult> describeInstanceStatusResultList = new ArrayList<>();

        List<List<String>> partitionedInstanceIdsList = Lists.partition(instanceIds, MAX_INSTANCE_ID_SIZE);

        for (List<String> partitionedInstanceIds : partitionedInstanceIdsList) {
            DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest().withInstanceIds(partitionedInstanceIds);
            DescribeInstanceStatusResult describeResult = amazonEC2Client.describeInstanceStatus(describeInstanceStatusRequest);
            describeInstanceStatusResultList.add(describeResult);
        }

        List<InstanceStatus> instanceStatusList = describeInstanceStatusResultList.stream()
                .flatMap(statusResult -> statusResult.getInstanceStatuses().stream())
                .collect(Collectors.toList());

        if (instanceStatusList.size() < requiredInstances) {
            LOGGER.debug("Instances up: {}, needed: {}", instanceStatusList.size(), requiredInstances);
            return false;
        }

        for (InstanceStatus status : instanceStatusList) {
            if (INSTANCE_RUNNING != status.getInstanceState().getCode()) {
                LOGGER.debug("Instances are up but not all of them are in running state.");
                return false;
            }
        }
        return true;
    }

    private void updateLatestActivity(List<Activity> activities) {
        if (!activities.isEmpty()) {
            latestActivity = Optional.ofNullable(activities.get(0));
        }
    }

    private void checkFailedActivities(Iterable<Activity> activities) {
        for (Activity activity : activities) {
            if (activity.getProgress().equals(COMPLETED) && (CANCELLED.equals(activity.getStatusCode()) || FAILED.equals(activity.getStatusCode()))) {
                throw new CloudConnectorException(activity.getStatusMessage());
            }
        }
    }

    private void checkForSpotRequest(Activity activity, AmazonEC2 amazonEC2Client) {
        if (WAIT_FOR_SPOT_INSTANCES_STATUS_CODE.equals(activity.getStatusCode())) {
            Pattern pattern = Pattern.compile(SPOT_ID_PATTERN);
            Matcher matcher = pattern.matcher(activity.getStatusMessage());
            if (matcher.find()) {
                String spotId = matcher.group(0);
                DescribeSpotInstanceRequestsResult spotResult = amazonEC2Client.describeSpotInstanceRequests(
                        new DescribeSpotInstanceRequestsRequest().withSpotInstanceRequestIds(spotId));
                Optional<SpotInstanceRequest> request = spotResult.getSpotInstanceRequests().stream().findFirst();
                if (request.isPresent()) {
                    if (LOW_SPOT_PRICE_STATUS_CODE.equals(request.get().getStatus().getCode())) {
                        throw new CloudConnectorException(request.get().getStatus().getMessage());
                    }
                }

            }
        }
    }

    private List<Activity> getAutoScalingActivities() {
        DescribeScalingActivitiesRequest describeScalingActivitiesRequest =
                new DescribeScalingActivitiesRequest().withAutoScalingGroupName(autoScalingGroupName);
        return autoScalingClient.describeScalingActivities(describeScalingActivitiesRequest).getActivities();
    }
}
