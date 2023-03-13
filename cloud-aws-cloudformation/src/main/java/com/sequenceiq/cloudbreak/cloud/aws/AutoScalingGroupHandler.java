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
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Component
public class AutoScalingGroupHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScalingGroupHandler.class);

    public void updateAutoScalingGroupWithLaunchConfiguration(AmazonAutoScalingClient autoScalingClient, String autoScalingGroupName,
            LaunchConfiguration oldLaunchConfiguration, String launchConfigurationName) {
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
            AmazonAutoScalingClient autoScalingClient, String stackName) {
        DescribeStackResourcesRequest resourcesRequest = DescribeStackResourcesRequest.builder().stackName(stackName).build();
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
            AmazonAutoScalingClient autoScalingClient, String stackName, String groupName) {
        Map<AutoScalingGroup, String> autoScalingGroups = getAutoScalingGroups(cloudFormationClient, autoScalingClient, stackName);
        return autoScalingGroups.keySet().stream().filter(a -> a.autoScalingGroupName().equals(groupName)).findFirst();
    }
}
