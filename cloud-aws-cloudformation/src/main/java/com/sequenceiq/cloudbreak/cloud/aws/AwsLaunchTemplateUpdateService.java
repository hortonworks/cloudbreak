package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Group;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupResponse;
import software.amazon.awssdk.services.ec2.model.AttributeValue;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionRequest;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionResponse;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.RequestLaunchTemplateData;

@Service
public class AwsLaunchTemplateUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLaunchTemplateUpdateService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    public void updateFieldsOnAllLaunchTemplate(AuthenticatedContext authenticatedContext, String stackName, Map<LaunchTemplateField, String> updatableFields) {
        updateFieldsOnAllLaunchTemplate(authenticatedContext, stackName, updatableFields, false);
    }

    public void updateFieldsOnAllLaunchTemplate(AuthenticatedContext authenticatedContext, String stackName,
            Map<LaunchTemplateField, String> updatableFields, boolean dryRun) {
        AmazonAutoScalingClient autoScalingClient = getAutoScalingClient(authenticatedContext);
        AmazonEc2Client ec2Client = getEc2Client(authenticatedContext);
        Map<AutoScalingGroup, String> autoScalingGroups = getAutoScalingGroups(authenticatedContext, stackName, dryRun);
        LOGGER.debug("Modifying the {} fields for the [{}] autoscaling groups' launchtemplates [dryrun: {}]",
                updatableFields,
                autoScalingGroups.values(),
                dryRun);
        for (Map.Entry<AutoScalingGroup, String> asgEntry : autoScalingGroups.entrySet()) {
            updateLaunchTemplate(updatableFields, dryRun, autoScalingClient, ec2Client, asgEntry.getKey());
        }
    }

    public Map<AutoScalingGroup, String> getAutoScalingGroups(AuthenticatedContext authenticatedContext, String stackName, boolean dryRun) {
        AmazonCloudFormationClient cloudFormationClient = getAmazonCloudFormationClient(authenticatedContext);
        AmazonAutoScalingClient autoScalingClient = getAutoScalingClient(authenticatedContext);
        Map<AutoScalingGroup, String> autoScalingGroups = autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, stackName);
        return filterGroupsForDryRun(autoScalingGroups, dryRun);
    }

    private AmazonAutoScalingClient getAutoScalingClient(AuthenticatedContext authenticatedContext) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        return awsClient.createAutoScalingClient(credentialView, regionName);
    }

    private AmazonCloudFormationClient getAmazonCloudFormationClient(AuthenticatedContext authenticatedContext) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        return awsClient.createCloudFormationClient(credentialView, regionName);
    }

    private AmazonEc2Client getEc2Client(AuthenticatedContext authenticatedContext) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        return awsClient.createEc2Client(credentialView, regionName);
    }

    public void updateLaunchTemplate(Map<LaunchTemplateField, String> updatableFields, boolean dryRun,
            AmazonAutoScalingClient autoScalingClient, AmazonEc2Client ec2Client, AutoScalingGroup asgEntry) {
        LOGGER.debug("Creating new launchtemplate version for [{}] autoscale group...", asgEntry.autoScalingGroupName());
        LaunchTemplateSpecification launchTemplateSpecification = getLaunchTemplateSpecification(asgEntry);
        CreateLaunchTemplateVersionResponse createLaunchTemplateVersionResponse = getCreateLaunchTemplateVersionRequest(ec2Client, updatableFields,
                launchTemplateSpecification);
        modifyLaunchTemplate(ec2Client, launchTemplateSpecification, createLaunchTemplateVersionResponse, !dryRun);
        if (dryRun) {
            LOGGER.debug("Autoscale group update will be skipped because of dryrun, which just test the permissions for modifiying the launch template.");
        } else {
            updateAutoScalingGroup(updatableFields, autoScalingClient, asgEntry, launchTemplateSpecification, createLaunchTemplateVersionResponse);
        }
    }

    public void updateLaunchTemplate(Map<LaunchTemplateField, String> updatableFields, AuthenticatedContext ac, String stackName, Group group) {
        AmazonCloudFormationClient amazonCloudFormationClient = getAmazonCloudFormationClient(ac);
        AmazonAutoScalingClient autoScalingClient = getAutoScalingClient(ac);
        AmazonEc2Client ec2Client = getEc2Client(ac);
        String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
        Optional<AutoScalingGroup> autoScalingGroupOptional = autoScalingGroupHandler
                .getAutoScalingGroup(amazonCloudFormationClient, autoScalingClient, stackName, asGroupName);
        if (autoScalingGroupOptional.isPresent()) {
            AutoScalingGroup autoScalingGroup = autoScalingGroupOptional.get();
            LOGGER.info("Autoscaling group presented with {} name so updating it.", autoScalingGroup.autoScalingGroupName());

            LOGGER.info("Get launch template specification for {} autoscaling group", autoScalingGroup.autoScalingGroupName());
            LaunchTemplateSpecification launchTemplateSpecification = getLaunchTemplateSpecification(autoScalingGroup);
            LOGGER.info("Update launch template specification for {} autoscaling group", autoScalingGroup.autoScalingGroupName());
            CreateLaunchTemplateVersionResponse newLaunchTemplate = getCreateLaunchTemplateVersionRequest(ec2Client, updatableFields,
                    launchTemplateSpecification);
            LOGGER.info("Set the new launchtemplate version on autoscaling group for {} autoscaling group", autoScalingGroup.autoScalingGroupName());
            setLaunchTemplateNewVersionAsDefault(ec2Client, launchTemplateSpecification, newLaunchTemplate);
            updateAutoScalingGroup(updatableFields,
                    autoScalingClient,
                    autoScalingGroup,
                    launchTemplateSpecification,
                    newLaunchTemplate);
            LOGGER.info("Update all instance for {} autoscaling group", autoScalingGroup.autoScalingGroupName());
            updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group);
        }
    }

    private void setLaunchTemplateNewVersionAsDefault(AmazonEc2Client ec2Client, LaunchTemplateSpecification launchTemplateSpecification,
            CreateLaunchTemplateVersionResponse createLaunchTemplateVersionResponse) {
        String version = createLaunchTemplateVersionResponse.launchTemplateVersion().versionNumber().toString();
        ModifyLaunchTemplateRequest modifyLaunchTemplateRequest = ModifyLaunchTemplateRequest.builder()
                .launchTemplateId(launchTemplateSpecification.launchTemplateId())
                .defaultVersion(version)
                .build();
        LOGGER.info("The new Launchtemplate version is {} on {} launchtemplate", version, launchTemplateSpecification.launchTemplateId());
        ec2Client.modifyLaunchTemplate(modifyLaunchTemplateRequest);
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
        LaunchTemplateSpecification launchTemplateSpecification = autoScalingGroup.launchTemplate() == null ?
                autoScalingGroup.mixedInstancesPolicy().launchTemplate().launchTemplateSpecification() : autoScalingGroup.launchTemplate();
        LOGGER.debug("Current launch template specification {} for autoScaling group {}", launchTemplateSpecification, autoScalingGroup);
        return launchTemplateSpecification;
    }

    private CreateLaunchTemplateVersionResponse getCreateLaunchTemplateVersionRequest(AmazonEc2Client ec2Client,
            Map<LaunchTemplateField, String> updatableFields, LaunchTemplateSpecification launchTemplateSpecification) {
        CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest = CreateLaunchTemplateVersionRequest.builder()
                .launchTemplateId(launchTemplateSpecification.launchTemplateId())
                .sourceVersion(launchTemplateSpecification.version())
                .versionDescription(updatableFields.getOrDefault(LaunchTemplateField.DESCRIPTION, null))
                .launchTemplateData(RequestLaunchTemplateData.builder()
                        .imageId(updatableFields.getOrDefault(LaunchTemplateField.IMAGE_ID, null))
                        .userData(updatableFields.getOrDefault(LaunchTemplateField.USER_DATA, null))
                        .instanceType(updatableFields.getOrDefault(LaunchTemplateField.INSTANCE_TYPE, null)).build())
                .build();
        CreateLaunchTemplateVersionResponse createLaunchTemplateVersionResponse = ec2Client.createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
        validateCreatedLaunchTemplateVersionResponse(createLaunchTemplateVersionResponse);
        LOGGER.debug("Updated field in new launch template version: {}", updatableFields);
        return createLaunchTemplateVersionResponse;
    }

    private void validateCreatedLaunchTemplateVersionResponse(CreateLaunchTemplateVersionResponse createLaunchTemplateVersionResponse) {
        if (createLaunchTemplateVersionResponse.warning() != null && CollectionUtils.isNotEmpty(createLaunchTemplateVersionResponse.warning().errors())) {
            String errorMsg = "Errors during launchtemplate version creation: " + createLaunchTemplateVersionResponse.warning().errors();
            LOGGER.warn(errorMsg);
            throw new CloudConnectorException(errorMsg);
        }
    }

    private ModifyLaunchTemplateResponse modifyLaunchTemplate(AmazonEc2Client ec2Client, LaunchTemplateSpecification launchTemplateSpecification,
            CreateLaunchTemplateVersionResponse createLaunchTemplateVersionResponse, boolean setLaunchtemplateVersionToDefault) {
        ModifyLaunchTemplateRequest.Builder modifyLaunchTemplateRequestBuilder = ModifyLaunchTemplateRequest.builder()
                .launchTemplateId(launchTemplateSpecification.launchTemplateId());
        if (setLaunchtemplateVersionToDefault) {
            modifyLaunchTemplateRequestBuilder.defaultVersion(createLaunchTemplateVersionResponse.launchTemplateVersion().versionNumber().toString());
        } else {
            LOGGER.debug("In case of dryrun the default version of the used launch template remain the same: {}", launchTemplateSpecification.version());
            modifyLaunchTemplateRequestBuilder.defaultVersion(launchTemplateSpecification.version());
        }
        ModifyLaunchTemplateResponse modifyLaunchTemplateResponse = ec2Client.modifyLaunchTemplate(modifyLaunchTemplateRequestBuilder.build());
        LOGGER.debug("Modified launch template: {}", modifyLaunchTemplateResponse);
        return modifyLaunchTemplateResponse;
    }

    private UpdateAutoScalingGroupResponse updateAutoScalingGroup(Map<LaunchTemplateField, String> updatableFields,
            AmazonAutoScalingClient autoScalingClient,
            AutoScalingGroup autoScalingGroup,
            LaunchTemplateSpecification launchTemplateSpecification,
            CreateLaunchTemplateVersionResponse createLaunchTemplateVersionResponse) {
        LaunchTemplateSpecification newLaunchTemplateSpecification = LaunchTemplateSpecification.builder()
                .launchTemplateId(launchTemplateSpecification.launchTemplateId())
                .version(createLaunchTemplateVersionResponse.launchTemplateVersion().versionNumber().toString())
                .build();
        UpdateAutoScalingGroupResponse updateAutoScalingGroupResponse = autoScalingClient.updateAutoScalingGroup(UpdateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(autoScalingGroup.autoScalingGroupName())
                .launchTemplate(newLaunchTemplateSpecification)
                .build());
        LOGGER.info("Create new LauncTemplateVersion {} with new fields {} and attached it to the {} autoscaling group.", newLaunchTemplateSpecification,
                updatableFields, autoScalingGroup.autoScalingGroupName());
        return updateAutoScalingGroupResponse;
    }

    private void updateInstanceInAutoscalingGroup(AmazonEc2Client ec2Client, AutoScalingGroup autoScalingGroup, Group group) {
        for (Instance instance : autoScalingGroup.instances()) {
            String requestedFlavor = group.getReferenceInstanceTemplate().getFlavor();
            LOGGER.info("Instance with name: {}, will use the new type which is: {}", instance.instanceId(),
                    requestedFlavor);
            if (!instance.instanceType().equals(requestedFlavor)) {
                ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = ModifyInstanceAttributeRequest.builder()
                        .instanceId(instance.instanceId())
                        .instanceType(AttributeValue.builder().value(requestedFlavor).build())
                        .build();
                ec2Client.modifyInstanceAttribute(modifyInstanceAttributeRequest);
            } else {
                LOGGER.info("Instance {} using the same type what was requested: {}", instance.instanceId(), requestedFlavor);
            }
        }
    }
}
