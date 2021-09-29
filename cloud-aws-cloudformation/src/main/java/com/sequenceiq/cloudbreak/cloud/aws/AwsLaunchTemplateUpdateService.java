package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionResult;
import com.amazonaws.services.ec2.model.ModifyLaunchTemplateRequest;
import com.amazonaws.services.ec2.model.ModifyLaunchTemplateResult;
import com.amazonaws.services.ec2.model.RequestLaunchTemplateData;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Service
public class AwsLaunchTemplateUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLaunchTemplateUpdateService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    public void updateFields(AuthenticatedContext authenticatedContext, String stackName, Map<LaunchTemplateField, String> updatableFields) {
        updateFields(authenticatedContext, stackName, updatableFields, false);
    }

    public void updateFields(AuthenticatedContext authenticatedContext, String stackName, Map<LaunchTemplateField, String> updatableFields,
            boolean dryRun) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(credentialView, regionName);
        AmazonEc2Client ec2Client = awsClient.createEc2Client(credentialView, regionName);
        Map<AutoScalingGroup, String> autoScalingGroups = autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, stackName);
        autoScalingGroups = filterGroupsForDryRun(autoScalingGroups, dryRun);
        LOGGER.debug("Modifying the {} fields for the [{}] autoscaling groups' launchtemplates [dryrun: {}]", updatableFields, autoScalingGroups.values(),
                dryRun);
        for (Map.Entry<AutoScalingGroup, String> asgEntry : autoScalingGroups.entrySet()) {
            AutoScalingGroup autoScalingGroup = asgEntry.getKey();
            LOGGER.debug("Creating new launchtemplate version for [{}] autoscale group...", autoScalingGroup.getAutoScalingGroupName());
            LaunchTemplateSpecification launchTemplateSpecification = getLaunchTemplateSpecification(autoScalingGroup);
            CreateLaunchTemplateVersionResult createLaunchTemplateVersionResult = getCreateLaunchTemplateVersionRequest(ec2Client, updatableFields,
                    launchTemplateSpecification);
            modifyLaunchTemplate(ec2Client, launchTemplateSpecification, createLaunchTemplateVersionResult, !dryRun);
            if (dryRun) {
                LOGGER.debug("Autoscale group update will be skipped because of dryrun, which just test the permissions for modifiying the launch template.");
            } else {
                updateAutoScalingGroup(updatableFields, autoScalingClient, autoScalingGroup, launchTemplateSpecification, createLaunchTemplateVersionResult);
            }
        }
    }

    private Map<AutoScalingGroup, String> filterGroupsForDryRun(Map<AutoScalingGroup, String> autoScalingGroups, boolean dryRun) {
        if (dryRun) {
            Optional<Map.Entry<AutoScalingGroup, String>> firstGroup = autoScalingGroups.entrySet().stream().findFirst();
            Map<AutoScalingGroup, String> result;
            if (firstGroup.isPresent()) {
                Map.Entry<AutoScalingGroup, String> firstGroupEntry = firstGroup.get();
                result = Map.of(firstGroupEntry.getKey(), firstGroupEntry.getValue());
            } else {
                result = Collections.emptyMap();
            }
            LOGGER.debug("In case of dryrun just one autoscale group's launchtemplate will be changed: [{}]", result);
            return result;
        } else {
            return autoScalingGroups;
        }
    }

    private LaunchTemplateSpecification getLaunchTemplateSpecification(AutoScalingGroup autoScalingGroup) {
        LaunchTemplateSpecification launchTemplateSpecification = autoScalingGroup.getLaunchTemplate() == null ?
                autoScalingGroup.getMixedInstancesPolicy().getLaunchTemplate().getLaunchTemplateSpecification() : autoScalingGroup.getLaunchTemplate();
        LOGGER.debug("Current launch template specification {} for autoScaling group {}", launchTemplateSpecification, autoScalingGroup);
        return launchTemplateSpecification;
    }

    private CreateLaunchTemplateVersionResult getCreateLaunchTemplateVersionRequest(AmazonEc2Client ec2Client,
            Map<LaunchTemplateField, String> updatableFields, LaunchTemplateSpecification launchTemplateSpecification) {
        CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest = new CreateLaunchTemplateVersionRequest()
                .withLaunchTemplateId(launchTemplateSpecification.getLaunchTemplateId())
                .withSourceVersion(launchTemplateSpecification.getVersion())
                .withVersionDescription(updatableFields.getOrDefault(LaunchTemplateField.DESCRIPTION, null))
                .withLaunchTemplateData(new RequestLaunchTemplateData()
                        .withImageId(updatableFields.getOrDefault(LaunchTemplateField.IMAGE_ID, null))
                        .withUserData(updatableFields.getOrDefault(LaunchTemplateField.USER_DATA, null)));
        CreateLaunchTemplateVersionResult createLaunchTemplateVersionResult = ec2Client.createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
        validateCreatedLaunchTemplateVersionResult(createLaunchTemplateVersionResult);
        LOGGER.debug("Updated field in new launch template version: {}", updatableFields);
        return createLaunchTemplateVersionResult;
    }

    private void validateCreatedLaunchTemplateVersionResult(CreateLaunchTemplateVersionResult createLaunchTemplateVersionResult) {
        if (createLaunchTemplateVersionResult.getWarning() != null && CollectionUtils.isNotEmpty(createLaunchTemplateVersionResult.getWarning().getErrors())) {
            String errorMsg = "Errors during launchtemplate version creation: " + createLaunchTemplateVersionResult.getWarning().getErrors();
            LOGGER.warn(errorMsg);
            throw new CloudConnectorException(errorMsg);
        }
    }

    private ModifyLaunchTemplateResult modifyLaunchTemplate(AmazonEc2Client ec2Client, LaunchTemplateSpecification launchTemplateSpecification,
            CreateLaunchTemplateVersionResult createLaunchTemplateVersionResult, boolean setLaunchtemplateVersionToDefault) {
        ModifyLaunchTemplateRequest modifyLaunchTemplateRequest = new ModifyLaunchTemplateRequest()
                .withLaunchTemplateId(launchTemplateSpecification.getLaunchTemplateId());
        if (setLaunchtemplateVersionToDefault) {
            modifyLaunchTemplateRequest.withDefaultVersion(createLaunchTemplateVersionResult.getLaunchTemplateVersion().getVersionNumber().toString());
        } else {
            LOGGER.debug("In case of dryrun the default version of the used launch template remain the same: {}", launchTemplateSpecification.getVersion());
            modifyLaunchTemplateRequest.withDefaultVersion(launchTemplateSpecification.getVersion());
        }
        ModifyLaunchTemplateResult modifyLaunchTemplateResult = ec2Client.modifyLaunchTemplate(modifyLaunchTemplateRequest);
        LOGGER.debug("Modified launch template: {}", modifyLaunchTemplateResult);
        return modifyLaunchTemplateResult;
    }

    private UpdateAutoScalingGroupResult updateAutoScalingGroup(Map<LaunchTemplateField, String> updatableFields, AmazonAutoScalingClient autoScalingClient,
            AutoScalingGroup autoScalingGroup, LaunchTemplateSpecification launchTemplateSpecification,
            CreateLaunchTemplateVersionResult createLaunchTemplateVersionResult) {
        LaunchTemplateSpecification newLaunchTemplateSpecification = new LaunchTemplateSpecification()
                .withLaunchTemplateId(launchTemplateSpecification.getLaunchTemplateId())
                .withVersion(createLaunchTemplateVersionResult.getLaunchTemplateVersion().getVersionNumber().toString());
        UpdateAutoScalingGroupResult updateAutoScalingGroupResult = autoScalingClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroup.getAutoScalingGroupName())
                .withLaunchTemplate(newLaunchTemplateSpecification));
        LOGGER.info("Create new LauncTemplateVersion {} with new fields {} and attached it to the {} autoscaling group.", newLaunchTemplateSpecification,
                updatableFields, autoScalingGroup.getAutoScalingGroupName());
        return updateAutoScalingGroupResult;
    }
}
