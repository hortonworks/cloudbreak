package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Component
public class AutoScalingGroupHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScalingGroupHandler.class);

    public void updateAutoScalingGroupWithLaunchConfiguration(AmazonAutoScalingClient autoScalingClient, String autoScalingGroupName,
            String launchConfigurationName) {
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = UpdateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroupName)
                .launchConfigurationName(launchConfigurationName)
                .build();
        LOGGER.debug("Update AutoScalingGroup {} with LaunchConfiguration {}",
                updateAutoScalingGroupRequest.autoScalingGroupName(), updateAutoScalingGroupRequest.launchConfigurationName());
        autoScalingClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    }

    public Map<AutoScalingGroup, String> getAutoScalingGroups(AmazonCloudFormationClient cloudFormationClient,
            AmazonAutoScalingClient autoScalingClient, CloudResource cfResource) {
        return getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource.getName());
    }

    public Map<AutoScalingGroup, String> getAutoScalingGroups(AmazonCloudFormationClient cloudFormationClient,
            AmazonAutoScalingClient autoScalingClient, String cloudFormationStackName) {
        DescribeStackResourcesRequest resourcesRequest = DescribeStackResourcesRequest.builder().stackName(cloudFormationStackName).build();
        DescribeStackResourcesResponse resourcesResult = cloudFormationClient.describeStackResources(resourcesRequest);
        Map<String, String> autoScalingGroups = resourcesResult.stackResources().stream()
                .filter(stackResource -> "AWS::AutoScaling::AutoScalingGroup".equalsIgnoreCase(stackResource.resourceType()))
                .collect(Collectors.toMap(StackResource::physicalResourceId, StackResource::logicalResourceId));
        DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(autoScalingGroups.keySet()).build();
        List<AutoScalingGroup> scalingGroups = autoScalingClient.describeAutoScalingGroups(request).autoScalingGroups();
        return scalingGroups.stream()
                .collect(Collectors.toMap(scalingGroup -> scalingGroup, scalingGroup -> autoScalingGroups.get(scalingGroup.autoScalingGroupName())));
    }

    public Optional<AutoScalingGroup> getAutoScalingGroup(AmazonCloudFormationClient cloudFormationClient,
            AmazonAutoScalingClient autoScalingClient, String cloudFormationStackName, String groupName) {
        Map<AutoScalingGroup, String> autoScalingGroups = getAutoScalingGroups(cloudFormationClient, autoScalingClient, cloudFormationStackName);
        return autoScalingGroups.keySet().stream().filter(a -> a.autoScalingGroupName().equals(groupName)).findFirst();
    }

    public Map<String, AutoScalingGroup> autoScalingGroupByName(AmazonCloudFormationClient cloudFormationRetryClient,
            AmazonAutoScalingClient autoScalingClient, String cloudFormationStackName) {
        Map<AutoScalingGroup, String> autoScalingGroups = getAutoScalingGroups(cloudFormationRetryClient, autoScalingClient, cloudFormationStackName);

        return autoScalingGroups.keySet().stream()
                .collect(Collectors.toMap(AutoScalingGroup::autoScalingGroupName, asg -> asg));
    }
}
