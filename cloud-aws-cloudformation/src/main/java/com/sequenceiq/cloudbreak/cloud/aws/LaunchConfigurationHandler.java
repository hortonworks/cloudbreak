package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.LaunchConfigurationMapper;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class LaunchConfigurationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchConfigurationHandler.class);

    @Inject
    private LaunchConfigurationMapper launchConfigurationMapper;

    @Inject
    private ResourceNotifier resourceNotifier;

    public List<LaunchConfiguration> getLaunchConfigurations(AmazonAutoScalingClient autoScalingClient, Collection<AutoScalingGroup> scalingGroups) {
        DescribeLaunchConfigurationsRequest launchConfigurationsRequest = new DescribeLaunchConfigurationsRequest();
        launchConfigurationsRequest.setLaunchConfigurationNames(
                scalingGroups.stream().map(AutoScalingGroup::getLaunchConfigurationName).collect(Collectors.toList()));
        return autoScalingClient.describeLaunchConfigurations(launchConfigurationsRequest).getLaunchConfigurations();
    }

    public String createNewLaunchConfiguration(String imageName, AmazonAutoScalingClient autoScalingClient,
            LaunchConfiguration oldLaunchConfiguration, CloudContext cloudContext) {
        CreateLaunchConfigurationRequest createLaunchConfigurationRequest = getCreateLaunchConfigurationRequest(imageName, oldLaunchConfiguration);
        LOGGER.debug("Create LaunchConfiguration {} with image {}",
                createLaunchConfigurationRequest.getLaunchConfigurationName(), imageName);
        autoScalingClient.createLaunchConfiguration(createLaunchConfigurationRequest);
        CloudResource cloudResource = CloudResource.builder()
                .type(ResourceType.AWS_LAUNCHCONFIGURATION)
                .params(Collections.emptyMap())
                .name(createLaunchConfigurationRequest.getLaunchConfigurationName())
                .availabilityZone(cloudContext.getLocation().getAvailabilityZone().value())
                .build();
        resourceNotifier.notifyAllocation(cloudResource, cloudContext);
        return createLaunchConfigurationRequest.getLaunchConfigurationName();
    }

    private CreateLaunchConfigurationRequest getCreateLaunchConfigurationRequest(String imageName, LaunchConfiguration oldLaunchConfiguration) {
        CreateLaunchConfigurationRequest createLaunchConfigurationRequest =
                launchConfigurationMapper.mapExistingLaunchConfigToRequest(oldLaunchConfiguration);
        createLaunchConfigurationRequest.setImageId(imageName);
        createLaunchConfigurationRequest.setLaunchConfigurationName(
                oldLaunchConfiguration.getLaunchConfigurationName().replaceAll("-ami-[a-z0-9]+", "") + '-' + imageName);
        return createLaunchConfigurationRequest;
    }

    public void removeOldLaunchConfiguration(LaunchConfiguration oldLaunchConfiguration, AmazonAutoScalingClient autoScalingClient,
            CloudContext cloudContext) {
        autoScalingClient.deleteLaunchConfiguration(
                new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(oldLaunchConfiguration.getLaunchConfigurationName()));
        CloudResource cloudResource = CloudResource.builder()
                .name(oldLaunchConfiguration.getLaunchConfigurationName())
                .availabilityZone(cloudContext.getLocation().getAvailabilityZone().value())
                .type(ResourceType.AWS_LAUNCHCONFIGURATION)
                .build();
        resourceNotifier.notifyDeletion(cloudResource, cloudContext);
    }
}
