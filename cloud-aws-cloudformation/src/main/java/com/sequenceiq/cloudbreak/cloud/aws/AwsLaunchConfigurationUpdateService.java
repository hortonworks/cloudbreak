package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;

@Service
public class AwsLaunchConfigurationUpdateService {

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private LaunchConfigurationHandler launchConfigurationHandler;

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private InstanceInAutoScalingGroupUpdater instanceUpdater;

    public void updateLaunchConfigurations(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource,
            Map<LaunchTemplateField, String> updatableFields) {
        updateLaunchConfigurationsForGroupsOrAll(authenticatedContext, stack, cfResource, updatableFields, null, true);
    }

    public void updateLaunchConfigurations(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource,
            Map<LaunchTemplateField, String> updatableFields, Group group, boolean updateInstances) {
        updateLaunchConfigurationsForGroupsOrAll(authenticatedContext, stack, cfResource, updatableFields, group, updateInstances);
    }

    private void updateLaunchConfigurationsForGroupsOrAll(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource,
            Map<LaunchTemplateField, String> updatableFields, Group group, boolean updateInstances) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(credentialView, regionName);

        Map<AutoScalingGroup, String> scalingGroups = group == null ?
                autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource)
                : filterForSingleAsgByGroup(authenticatedContext, cfResource, group, cloudFormationClient, autoScalingClient);
        List<LaunchConfiguration> oldLaunchConfigurations = launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroups.keySet());
        for (LaunchConfiguration oldLaunchConfiguration : oldLaunchConfigurations) {
            changelaunchConfigurationInAutoscalingGroup(authenticatedContext, stack, autoScalingClient, scalingGroups, oldLaunchConfiguration, updatableFields);
        }
        if (group != null && updateInstances) {
            AmazonEc2Client ec2Client = awsClient.createEc2Client(credentialView, regionName);
            scalingGroups.keySet().forEach(autoScalingGroup -> instanceUpdater.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group));
        }
    }

    private Map<AutoScalingGroup, String> filterForSingleAsgByGroup(AuthenticatedContext ac, CloudResource cfResource, Group group,
            AmazonCloudFormationClient cloudFormationClient, AmazonAutoScalingClient autoScalingClient) {
        String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
        return autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource).entrySet().stream()
                .filter(entry -> entry.getKey().autoScalingGroupName().equals(asGroupName))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void changelaunchConfigurationInAutoscalingGroup(AuthenticatedContext authenticatedContext, CloudStack stack,
            AmazonAutoScalingClient autoScalingClient, Map<AutoScalingGroup, String> scalingGroups, LaunchConfiguration oldLaunchConfiguration,
            Map<LaunchTemplateField, String> updatableFields) {

        Map.Entry<AutoScalingGroup, String> autoScalingGroup = getAutoScalingGroupForLaunchConfiguration(scalingGroups, oldLaunchConfiguration);

        String launchConfigurationName = launchConfigurationHandler.createNewLaunchConfiguration(updatableFields, autoScalingClient, oldLaunchConfiguration,
                authenticatedContext.getCloudContext(), authenticatedContext, stack);

        autoScalingGroupHandler.updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient, autoScalingGroup.getKey().autoScalingGroupName(),
                launchConfigurationName);

        launchConfigurationHandler.removeOldLaunchConfiguration(oldLaunchConfiguration, autoScalingClient, authenticatedContext.getCloudContext());
    }

    private Map.Entry<AutoScalingGroup, String> getAutoScalingGroupForLaunchConfiguration(Map<AutoScalingGroup, String> scalingGroups,
            LaunchConfiguration oldLaunchConfiguration) {
        return scalingGroups.entrySet().stream()
                .filter(entry -> entry.getKey().launchConfigurationName()
                        .equalsIgnoreCase(oldLaunchConfiguration.launchConfigurationName()))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Launch configuration not found for: "
                        + oldLaunchConfiguration.launchConfigurationName()));
    }
}
