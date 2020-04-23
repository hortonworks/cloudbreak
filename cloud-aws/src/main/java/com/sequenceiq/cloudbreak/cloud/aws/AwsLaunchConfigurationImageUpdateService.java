package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;

@Service
public class AwsLaunchConfigurationImageUpdateService {

    @Inject
    private AwsClient awsClient;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private EncryptedImageCopyService encryptedImageCopyService;

    @Inject
    private LaunchConfigurationHandler launchConfigurationHandler;

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(credentialView, regionName);

        Map<String, String> encryptedImages = getEncryptedImagesMappedByAutoscalingGroupName(authenticatedContext, stack);
        Map<AutoScalingGroup, String> scalingGroups = autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        List<LaunchConfiguration> oldLaunchConfigurations = launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroups.keySet());
        for (LaunchConfiguration oldLaunchConfiguration : oldLaunchConfigurations) {
            changeImageInAutoscalingGroup(authenticatedContext, stack, autoScalingClient, scalingGroups, encryptedImages, oldLaunchConfiguration);
        }
    }

    private Map<String, String> getEncryptedImagesMappedByAutoscalingGroupName(AuthenticatedContext authenticatedContext, CloudStack stack) {
        return encryptedImageCopyService.createEncryptedImages(authenticatedContext, stack, resourceNotifier).entrySet()
                .stream().collect(Collectors.toMap(entry -> AwsGroupView.getAutoScalingGroupName(entry.getKey()), Map.Entry::getValue));
    }

    private void changeImageInAutoscalingGroup(AuthenticatedContext authenticatedContext, CloudStack stack, AmazonAutoScalingClient autoScalingClient,
            Map<AutoScalingGroup, String> scalingGroups, Map<String, String> encryptedImages, LaunchConfiguration oldLaunchConfiguration) {

        Map.Entry<AutoScalingGroup, String> autoScalingGroup = getAutoScalingGroupForLaunchConfiguration(scalingGroups, oldLaunchConfiguration);

        String encryptedImageName = encryptedImages.get(autoScalingGroup.getValue());
        String launchConfigurationName = launchConfigurationHandler.createNewLaunchConfiguration(
                stack.getImage().getImageName(), autoScalingClient, oldLaunchConfiguration, authenticatedContext.getCloudContext(), encryptedImageName);

        autoScalingGroupHandler.updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient, autoScalingGroup.getKey().getAutoScalingGroupName(),
                oldLaunchConfiguration, launchConfigurationName);

        launchConfigurationHandler.removeOldLaunchConfiguration(oldLaunchConfiguration, autoScalingClient, authenticatedContext.getCloudContext());
    }

    private Map.Entry<AutoScalingGroup, String> getAutoScalingGroupForLaunchConfiguration(Map<AutoScalingGroup, String> scalingGroups,
            LaunchConfiguration oldLaunchConfiguration) {
        return scalingGroups.entrySet().stream()
                .filter(entry -> entry.getKey().getLaunchConfigurationName()
                        .equalsIgnoreCase(oldLaunchConfiguration.getLaunchConfigurationName()))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Launch configuration not found for: "
                        + oldLaunchConfiguration.getLaunchConfigurationName()));
    }
}
