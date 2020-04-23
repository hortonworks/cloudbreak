package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class AwsLaunchTemplateImageUpdateService {

    @Inject
    private AwsClient awsClient;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private EncryptedImageCopyService encryptedImageCopyService;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);

        String imageName = stack.getImage().getImageName();
        String cfStackName = cfResource.getName();
        String cfTemplate = getCfTemplate(cfResource, cloudFormationClient);
        Json templateJson = new Json(cfTemplate);

        Map<String, String> encryptedImages = getEncryptedImagesMappedByAutoscalingGroupName(authenticatedContext, stack);
        stack.getGroups().forEach(group -> {
            String imageIdPath = String.format("Resources.%s.Properties.LaunchTemplateData.ImageId", AwsGroupView.getLaunchTemplateName(group.getName()));
            Object oldImageId = templateJson.getValue(imageIdPath);
            boolean encryptedImage = !"{\"Ref\":\"AMI\"}".equals(oldImageId.toString());
            if (encryptedImage) {
                replaceEncryptedImageInGroup(encryptedImages, imageName, templateJson, group, imageIdPath);
            }
        });

        String newCfTemplate = templateJson.getValue();
        UpdateStackRequest updateStackRequest = awsStackRequestHelper.createUpdateStackRequest(authenticatedContext, stack, cfStackName, newCfTemplate);
        cloudFormationClient.updateStack(updateStackRequest);
    }

    private String getCfTemplate(CloudResource cfResource, AmazonCloudFormationClient cloudFormationClient) {
        String cfStackName = cfResource.getName();
        GetTemplateResult template = cloudFormationClient.getTemplate(new GetTemplateRequest().withStackName(cfStackName));
        return template.getTemplateBody();
    }

    private Map<String, String> getEncryptedImagesMappedByAutoscalingGroupName(AuthenticatedContext authenticatedContext, CloudStack stack) {
        return encryptedImageCopyService.createEncryptedImages(authenticatedContext, stack, resourceNotifier).entrySet()
                .stream().collect(Collectors.toMap(entry -> AwsGroupView.getAutoScalingGroupName(entry.getKey()), Map.Entry::getValue));
    }

    private void replaceEncryptedImageInGroup(Map<String, String> encryptedImages, String imageName, Json templateJson, Group group, String imageIdPath) {
        String autoScalingGroupName = AwsGroupView.getAutoScalingGroupName(group.getName());
        String encryptedImageName = encryptedImages.get(autoScalingGroupName);
        String selectedImageName = StringUtils.isBlank(encryptedImageName) ? imageName : encryptedImageName;
        templateJson.replaceValue(imageIdPath, selectedImageName);
    }
}
