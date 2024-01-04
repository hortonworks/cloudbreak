package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.LaunchConfigurationMapper;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.BlockDeviceMapping;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;

@Component
public class LaunchConfigurationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchConfigurationHandler.class);

    private static final int LAUNCHCONFIG_SUFFIX_LENGTH = 12;

    @Inject
    private LaunchConfigurationMapper launchConfigurationMapper;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private ResizedRootBlockDeviceMappingProvider resizedRootBlockDeviceMappingProvider;

    public List<LaunchConfiguration> getLaunchConfigurations(AmazonAutoScalingClient autoScalingClient, Collection<AutoScalingGroup> scalingGroups) {
        DescribeLaunchConfigurationsRequest launchConfigurationsRequest = DescribeLaunchConfigurationsRequest.builder()
                .launchConfigurationNames(scalingGroups.stream().map(AutoScalingGroup::launchConfigurationName).collect(Collectors.toList()))
                .build();
        return autoScalingClient.describeLaunchConfigurations(launchConfigurationsRequest).launchConfigurations();
    }

    public String createNewLaunchConfiguration(Map<LaunchTemplateField, String> updatableFields, AmazonAutoScalingClient autoScalingClient,
            LaunchConfiguration oldLaunchConfiguration, CloudContext cloudContext, AuthenticatedContext ac, CloudStack stack) {
        CreateLaunchConfigurationRequest createLaunchConfigurationRequest =
                getCreateLaunchConfigurationRequest(ac, stack, updatableFields, oldLaunchConfiguration);
        LOGGER.debug("Create LaunchConfiguration {} with {}",
                createLaunchConfigurationRequest.launchConfigurationName(), updatableFields);
        autoScalingClient.createLaunchConfiguration(createLaunchConfigurationRequest);
        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .withParameters(Collections.emptyMap())
                .withName(createLaunchConfigurationRequest.launchConfigurationName())
                .withAvailabilityZone(cloudContext.getLocation().getAvailabilityZone().value())
                .build();
        resourceNotifier.notifyAllocation(cloudResource, cloudContext);
        return createLaunchConfigurationRequest.launchConfigurationName();
    }

    private CreateLaunchConfigurationRequest getCreateLaunchConfigurationRequest(AuthenticatedContext ac, CloudStack cloudStack,
            Map<LaunchTemplateField, String> updatableFields,
            LaunchConfiguration oldLaunchConfiguration) {
        String imageName = updatableFields.getOrDefault(LaunchTemplateField.IMAGE_ID, oldLaunchConfiguration.imageId());
        String newLaunchConfigName = StringUtils.substringBeforeLast(
                oldLaunchConfiguration.launchConfigurationName().replaceAll("-ami-[a-z0-9]+", ""), "-")
                + "-" + PasswordUtil.generate(LAUNCHCONFIG_SUFFIX_LENGTH, true, true);
        CreateLaunchConfigurationRequest.Builder createLaunchConfigurationRequest =
                launchConfigurationMapper.mapExistingLaunchConfigToRequestBuilder(oldLaunchConfiguration)
                        .imageId(imageName)
                        .instanceType(updatableFields.getOrDefault(LaunchTemplateField.INSTANCE_TYPE, oldLaunchConfiguration.instanceType()))
                        .launchConfigurationName(newLaunchConfigName);
        Optional<List<BlockDeviceMapping>> blockDeviceMapping =
                resizedRootBlockDeviceMappingProvider.createBlockDeviceMappingIfRootDiskResizeRequired(ac, cloudStack, updatableFields, oldLaunchConfiguration);
        blockDeviceMapping.ifPresent(createLaunchConfigurationRequest::blockDeviceMappings);
        return createLaunchConfigurationRequest.build();
    }

    public void removeOldLaunchConfiguration(LaunchConfiguration oldLaunchConfiguration, AmazonAutoScalingClient autoScalingClient,
            CloudContext cloudContext) {
        autoScalingClient.deleteLaunchConfiguration(
                DeleteLaunchConfigurationRequest.builder()
                        .launchConfigurationName(oldLaunchConfiguration.launchConfigurationName())
                        .build());
        CloudResource cloudResource = CloudResource.builder()
                .withName(oldLaunchConfiguration.launchConfigurationName())
                .withAvailabilityZone(cloudContext.getLocation().getAvailabilityZone().value())
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .build();
        resourceNotifier.notifyDeletion(cloudResource, cloudContext);
    }
}
