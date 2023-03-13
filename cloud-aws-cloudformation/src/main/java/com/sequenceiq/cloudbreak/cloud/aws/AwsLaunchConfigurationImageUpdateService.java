package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;

@Service
public class AwsLaunchConfigurationImageUpdateService {

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private LaunchConfigurationHandler launchConfigurationHandler;

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(credentialView, regionName);

        Map<AutoScalingGroup, String> scalingGroups = autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        List<LaunchConfiguration> oldLaunchConfigurations = launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroups.keySet());
        for (LaunchConfiguration oldLaunchConfiguration : oldLaunchConfigurations) {
            changeImageInAutoscalingGroup(authenticatedContext, stack, autoScalingClient, scalingGroups, oldLaunchConfiguration);
        }
    }

    private void changeImageInAutoscalingGroup(AuthenticatedContext authenticatedContext, CloudStack stack, AmazonAutoScalingClient autoScalingClient,
            Map<AutoScalingGroup, String> scalingGroups, LaunchConfiguration oldLaunchConfiguration) {

        Map.Entry<AutoScalingGroup, String> autoScalingGroup = getAutoScalingGroupForLaunchConfiguration(scalingGroups, oldLaunchConfiguration);

        String launchConfigurationName = launchConfigurationHandler.createNewLaunchConfiguration(
                stack.getImage().getImageName(), autoScalingClient, oldLaunchConfiguration, authenticatedContext.getCloudContext());

        autoScalingGroupHandler.updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient, autoScalingGroup.getKey().autoScalingGroupName(),
                oldLaunchConfiguration, launchConfigurationName);

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
