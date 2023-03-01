package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.LaunchConfigurationMapper;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsResponse;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;

@ExtendWith(MockitoExtension.class)
class LaunchConfigurationHandlerTest {

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private LaunchConfigurationMapper launchConfigurationMapper;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @InjectMocks
    private LaunchConfigurationHandler underTest;

    @Test
    void getLaunchConfigurations() {
        when(autoScalingClient.describeLaunchConfigurations(any(DescribeLaunchConfigurationsRequest.class)))
                .thenReturn(DescribeLaunchConfigurationsResponse.builder().build());

        Collection<AutoScalingGroup> autoScalingGroups = Lists.newArrayList(AutoScalingGroup.builder().launchConfigurationName("a").build(),
                AutoScalingGroup.builder().launchConfigurationName("b").build());
        underTest.getLaunchConfigurations(autoScalingClient, autoScalingGroups);
        ArgumentCaptor<DescribeLaunchConfigurationsRequest> captor = ArgumentCaptor.forClass(DescribeLaunchConfigurationsRequest.class);
        verify(autoScalingClient, times(1)).describeLaunchConfigurations(captor.capture());
        assertEquals(autoScalingGroups.size(), captor.getValue().launchConfigurationNames().size());
    }

    @Test
    void createNewLaunchConfiguration() {
        String lName = "lName";
        when(launchConfigurationMapper.mapExistingLaunchConfigToRequestBuilder(any(LaunchConfiguration.class)))
                .thenReturn(CreateLaunchConfigurationRequest.builder().launchConfigurationName(lName));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform("AWS")
                .withWorkspaceId(WORKSPACE_ID)
                .withLocation(location(region("region"), availabilityZone("az1")))
                .build();
        String imageName = "imageName";
        String launchConfigurationName = underTest.createNewLaunchConfiguration(imageName, autoScalingClient,
                LaunchConfiguration.builder().launchConfigurationName(lName).build(), cloudContext);
        ArgumentCaptor<CreateLaunchConfigurationRequest> captor = ArgumentCaptor.forClass(CreateLaunchConfigurationRequest.class);
        verify(autoScalingClient).createLaunchConfiguration(captor.capture());
        assertTrue(captor.getValue().launchConfigurationName().startsWith(lName));
        assertTrue(captor.getValue().launchConfigurationName().endsWith(imageName));
        assertEquals(lName + '-' + imageName, launchConfigurationName);
        verify(resourceNotifier, times(1)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    void removeOldLaunchConfiguration() {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform("AWS")
                .withWorkspaceId(WORKSPACE_ID)
                .withLocation(location(region("region"), availabilityZone("az1")))
                .build();
        String launchConfigurationName = "old";
        underTest.removeOldLaunchConfiguration(LaunchConfiguration.builder().launchConfigurationName(launchConfigurationName).build(),
                autoScalingClient, context);
        ArgumentCaptor<DeleteLaunchConfigurationRequest> captor = ArgumentCaptor.forClass(DeleteLaunchConfigurationRequest.class);
        verify(autoScalingClient, times(1)).deleteLaunchConfiguration(captor.capture());
        assertEquals(launchConfigurationName, captor.getValue().launchConfigurationName());
        verify(resourceNotifier, times(1)).notifyDeletion(any(CloudResource.class), eq(context));
    }
}
