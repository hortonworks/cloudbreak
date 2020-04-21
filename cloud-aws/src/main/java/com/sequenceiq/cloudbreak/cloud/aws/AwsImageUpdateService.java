package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class AwsImageUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsImageUpdateService.class);

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

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(credentialView, regionName);

        String cfStackName = cfResource.getName();
        GetTemplateResult template = cloudFormationClient.getTemplate(new GetTemplateRequest().withStackName(cfStackName));
        String templateBody = template.getTemplateBody();

        Map<String, String> encryptedImages = getEncryptedImagesMappedByAutoscalingGroupName(authenticatedContext, stack);
        if (templateBody.contains("AWS::AutoScaling::LaunchConfiguration")) {
            updateImagesInLaunchConfigurations(authenticatedContext, stack, autoScalingClient, encryptedImages, cloudFormationClient, cfResource);
        } else if (templateBody.contains("AWS::EC2::LaunchTemplate")) {
            updateImagesInCloudFormationTemplate(authenticatedContext, cloudFormationClient, cfResource, encryptedImages, stack, templateBody);
        } else {
            throw new NotImplementedException("Image update for CF template is not implemented yet.");
        }
    }

    private void updateImagesInLaunchConfigurations(AuthenticatedContext authenticatedContext, CloudStack stack, AmazonAutoScalingClient autoScalingClient,
            Map<String, String> encryptedImages, AmazonCloudFormationClient cloudFormationClient, CloudResource cfResource) {
        Map<AutoScalingGroup, String> scalingGroups = autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        List<LaunchConfiguration> oldLaunchConfigurations = launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroups.keySet());
        for (LaunchConfiguration oldLaunchConfiguration : oldLaunchConfigurations) {
            changeImageInAutoscalingGroup(authenticatedContext, stack, autoScalingClient, scalingGroups, encryptedImages, oldLaunchConfiguration);
        }
    }

    private void changeImageInAutoscalingGroup(AuthenticatedContext authenticatedContext, CloudStack stack, AmazonAutoScalingClient autoScalingClient,
            Map<AutoScalingGroup, String> scalingGroups, Map<String, String> encryptedImages, LaunchConfiguration oldLaunchConfiguration) {

        Entry<AutoScalingGroup, String> autoScalingGroup = getAutoScalingGroupForLaunchConfiguration(scalingGroups, oldLaunchConfiguration);

        String encryptedImageName = encryptedImages.get(autoScalingGroup.getValue());
        String launchConfigurationName = launchConfigurationHandler.createNewLaunchConfiguration(
                stack.getImage().getImageName(), autoScalingClient, oldLaunchConfiguration, authenticatedContext.getCloudContext(), encryptedImageName);

        autoScalingGroupHandler.updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient, autoScalingGroup.getKey().getAutoScalingGroupName(),
                oldLaunchConfiguration, launchConfigurationName);

        launchConfigurationHandler.removeOldLaunchConfiguration(oldLaunchConfiguration, autoScalingClient, authenticatedContext.getCloudContext());
    }

    private Entry<AutoScalingGroup, String> getAutoScalingGroupForLaunchConfiguration(Map<AutoScalingGroup, String> scalingGroups,
            LaunchConfiguration oldLaunchConfiguration) {
        return scalingGroups.entrySet().stream()
                    .filter(entry -> entry.getKey().getLaunchConfigurationName()
                            .equalsIgnoreCase(oldLaunchConfiguration.getLaunchConfigurationName()))
                    .findFirst().orElseThrow(() -> new NoSuchElementException("Launch configuration not found for: "
                        + oldLaunchConfiguration.getLaunchConfigurationName()));
    }

    private Map<String, String> getEncryptedImagesMappedByAutoscalingGroupName(AuthenticatedContext authenticatedContext, CloudStack stack) {
        return encryptedImageCopyService.createEncryptedImages(authenticatedContext, stack, resourceNotifier).entrySet()
                    .stream().collect(Collectors.toMap(entry -> AwsGroupView.getAutoScalingGroupName(entry.getKey()), Entry::getValue));
    }

    private void updateImagesInCloudFormationTemplate(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cloudFormationClient,
            CloudResource cfResource, Map<String, String> encryptedImages, CloudStack stack, String templateBody) {
        String imageName = stack.getImage().getImageName();
        String cfStackName = cfResource.getName();
        Json templateJson = new Json(templateBody);

        stack.getGroups().forEach(group -> {
            String imageIdPath = String.format("Resources.%s.Properties.LaunchTemplateData.ImageId", AwsGroupView.getLaunchTemplateName(group.getName()));
            Object oldImageId = templateJson.getValue(imageIdPath);
            if (!"{\"Ref\":\"AMI\"}".equals(oldImageId.toString())) {
                String autoScalingGroupName = AwsGroupView.getAutoScalingGroupName(group.getName());
                String encryptedImageName = encryptedImages.get(autoScalingGroupName);
                String selectedImageName = StringUtils.isBlank(encryptedImageName) ? imageName : encryptedImageName;
                templateJson.replaceValue(imageIdPath, selectedImageName);
            }
        });

        String newTemplateBody = templateJson.getValue();
        UpdateStackRequest updateStackRequest = awsStackRequestHelper.createUpdateStackRequest(authenticatedContext, stack, cfStackName, newTemplateBody);
        cloudFormationClient.updateStack(updateStackRequest);
    }
}
