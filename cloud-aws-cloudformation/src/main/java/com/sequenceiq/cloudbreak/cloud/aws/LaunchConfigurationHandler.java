package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.LaunchConfigurationMapper;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;

@Component
public class LaunchConfigurationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchConfigurationHandler.class);

    @Inject
    private LaunchConfigurationMapper launchConfigurationMapper;

    @Inject
    private ResourceNotifier resourceNotifier;

    public List<LaunchConfiguration> getLaunchConfigurations(AmazonAutoScalingClient autoScalingClient, Collection<AutoScalingGroup> scalingGroups) {
        DescribeLaunchConfigurationsRequest launchConfigurationsRequest = DescribeLaunchConfigurationsRequest.builder()
                .launchConfigurationNames(scalingGroups.stream().map(AutoScalingGroup::launchConfigurationName).collect(Collectors.toList()))
                .build();
        return autoScalingClient.describeLaunchConfigurations(launchConfigurationsRequest).launchConfigurations();
    }

    public String createNewLaunchConfiguration(String imageName, AmazonAutoScalingClient autoScalingClient,
            LaunchConfiguration oldLaunchConfiguration, CloudContext cloudContext) {
        CreateLaunchConfigurationRequest createLaunchConfigurationRequest = getCreateLaunchConfigurationRequest(imageName, oldLaunchConfiguration);
        LOGGER.debug("Create LaunchConfiguration {} with image {}",
                createLaunchConfigurationRequest.launchConfigurationName(), imageName);
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

    private CreateLaunchConfigurationRequest getCreateLaunchConfigurationRequest(String imageName, LaunchConfiguration oldLaunchConfiguration) {
        CreateLaunchConfigurationRequest.Builder createLaunchConfigurationRequest =
                launchConfigurationMapper.mapExistingLaunchConfigToRequestBuilder(oldLaunchConfiguration)
                        .imageId(imageName)
                        .launchConfigurationName(
                                oldLaunchConfiguration.launchConfigurationName().replaceAll("-ami-[a-z0-9]+", "") + '-' + imageName);
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
