package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
public class AutoScalingGroupHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScalingGroupHandler.class);

    public void updateAutoScalingGroupWithLaunchConfiguration(AmazonAutoScalingClient autoScalingClient, String autoScalingGroupName,
            LaunchConfiguration oldLaunchConfiguration, String launchConfigurationName) {
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
        updateAutoScalingGroupRequest.setAutoScalingGroupName(autoScalingGroupName);
        updateAutoScalingGroupRequest.setLaunchConfigurationName(launchConfigurationName);
        LOGGER.debug("Update AutoScalingGroup {} with LaunchConfiguration {}",
                updateAutoScalingGroupRequest.getAutoScalingGroupName(), updateAutoScalingGroupRequest.getLaunchConfigurationName());
        autoScalingClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    }

    public Map<AutoScalingGroup, String> getAutoScalingGroups(AmazonCloudFormationClient cloudFormationClient,
            AmazonAutoScalingClient autoScalingClient, CloudResource cfResource) {
        return getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource.getName());
    }

    public Map<AutoScalingGroup, String> getAutoScalingGroups(AmazonCloudFormationClient cloudFormationClient,
            AmazonAutoScalingClient autoScalingClient, String stackName) {
        DescribeStackResourcesRequest resourcesRequest = new DescribeStackResourcesRequest();
        resourcesRequest.setStackName(stackName);
        DescribeStackResourcesResult resourcesResult = cloudFormationClient.describeStackResources(resourcesRequest);
        Map<String, String> autoScalingGroups = resourcesResult.getStackResources().stream()
                .filter(stackResource -> "AWS::AutoScaling::AutoScalingGroup".equalsIgnoreCase(stackResource.getResourceType()))
                .collect(Collectors.toMap(StackResource::getPhysicalResourceId, StackResource::getLogicalResourceId));
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(autoScalingGroups.keySet());
        List<AutoScalingGroup> scalingGroups = autoScalingClient.describeAutoScalingGroups(request).getAutoScalingGroups();
        return scalingGroups.stream()
                .collect(Collectors.toMap(scalingGroup -> scalingGroup, scalingGroup -> autoScalingGroups.get(scalingGroup.getAutoScalingGroupName())));
    }
}
