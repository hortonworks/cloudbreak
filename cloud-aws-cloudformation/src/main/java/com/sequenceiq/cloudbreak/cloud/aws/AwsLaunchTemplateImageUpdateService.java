package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionResult;
import com.amazonaws.services.ec2.model.ModifyLaunchTemplateRequest;
import com.amazonaws.services.ec2.model.RequestLaunchTemplateData;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AwsLaunchTemplateImageUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLaunchTemplateImageUpdateService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(credentialView, regionName);
        AmazonEc2Client ec2Client = awsClient.createEc2Client(credentialView, regionName);
        Map<AutoScalingGroup, String> autoScalingGroups = autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        for (Map.Entry<AutoScalingGroup, String> asgEntry : autoScalingGroups.entrySet()) {
            AutoScalingGroup autoScalingGroup = asgEntry.getKey();
            LaunchTemplateSpecification launchTemplateSpecification = autoScalingGroup.getLaunchTemplate() == null ?
                    autoScalingGroup.getMixedInstancesPolicy().getLaunchTemplate().getLaunchTemplateSpecification() : autoScalingGroup.getLaunchTemplate();
            CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest = new CreateLaunchTemplateVersionRequest()
                    .withLaunchTemplateId(launchTemplateSpecification.getLaunchTemplateId())
                    .withSourceVersion(launchTemplateSpecification.getVersion())
                    .withLaunchTemplateData(new RequestLaunchTemplateData().withImageId(stack.getImage().getImageName()));
            CreateLaunchTemplateVersionResult createLaunchTemplateVersionResult = ec2Client.createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
            ModifyLaunchTemplateRequest modifyLaunchTemplateRequest = new ModifyLaunchTemplateRequest()
                    .withLaunchTemplateId(launchTemplateSpecification.getLaunchTemplateId())
                    .withDefaultVersion(createLaunchTemplateVersionResult.getLaunchTemplateVersion().getVersionNumber().toString());
            ec2Client.modifyLaunchTemplate(modifyLaunchTemplateRequest);
            LaunchTemplateSpecification newLaunchTemplateSpecification = new LaunchTemplateSpecification()
                    .withLaunchTemplateId(launchTemplateSpecification.getLaunchTemplateId())
                    .withVersion(createLaunchTemplateVersionResult.getLaunchTemplateVersion().getVersionNumber().toString());
            autoScalingClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(autoScalingGroup.getAutoScalingGroupName())
                    .withLaunchTemplate(newLaunchTemplateSpecification));
            LOGGER.info("Create new LauncTemplateVersion {} with new imageid {} and attached it to the {} autoscaling group.", newLaunchTemplateSpecification,
                    stack.getImage().getImageName(), autoScalingGroup.getAutoScalingGroupName());
        }
    }
}
