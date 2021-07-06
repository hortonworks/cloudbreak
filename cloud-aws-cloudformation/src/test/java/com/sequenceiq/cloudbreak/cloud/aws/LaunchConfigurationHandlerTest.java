package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.LaunchConfigurationMapper;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;

public class LaunchConfigurationHandlerTest {

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private LaunchConfigurationMapper launchConfigurationMapper;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @InjectMocks
    private LaunchConfigurationHandler underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getLaunchConfigurations() {
        when(autoScalingClient.describeLaunchConfigurations(any(DescribeLaunchConfigurationsRequest.class)))
                .thenReturn(new DescribeLaunchConfigurationsResult());

        Collection<AutoScalingGroup> autoScalingGroups = Lists.newArrayList(new AutoScalingGroup().withLaunchConfigurationName("a"),
                new AutoScalingGroup().withLaunchConfigurationName("b"));
        underTest.getLaunchConfigurations(autoScalingClient, autoScalingGroups);
        ArgumentCaptor<DescribeLaunchConfigurationsRequest> captor = ArgumentCaptor.forClass(DescribeLaunchConfigurationsRequest.class);
        verify(autoScalingClient, times(1)).describeLaunchConfigurations(captor.capture());
        assertEquals(autoScalingGroups.size(), captor.getValue().getLaunchConfigurationNames().size());
    }

    @Test
    public void createNewLaunchConfiguration() {
        String lName = "lName";
        when(launchConfigurationMapper.mapExistingLaunchConfigToRequest(any(LaunchConfiguration.class)))
                .thenReturn(new CreateLaunchConfigurationRequest().withLaunchConfigurationName(lName));
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
                new LaunchConfiguration().withLaunchConfigurationName(lName), cloudContext);
        ArgumentCaptor<CreateLaunchConfigurationRequest> captor = ArgumentCaptor.forClass(CreateLaunchConfigurationRequest.class);
        verify(autoScalingClient).createLaunchConfiguration(captor.capture());
        assertTrue(captor.getValue().getLaunchConfigurationName().startsWith(lName));
        assertTrue(captor.getValue().getLaunchConfigurationName().endsWith(imageName));
        assertEquals(lName + '-' + imageName, launchConfigurationName);
        verify(resourceNotifier, times(1)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    public void removeOldLaunchConfiguration() {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform("AWS")
                .withWorkspaceId(WORKSPACE_ID)
                .withLocation(location(region("region"), availabilityZone("az1")))
                .build();
        String launchConfigurationName = "old";
        underTest.removeOldLaunchConfiguration(new LaunchConfiguration().withLaunchConfigurationName(launchConfigurationName), autoScalingClient, context);
        ArgumentCaptor<DeleteLaunchConfigurationRequest> captor = ArgumentCaptor.forClass(DeleteLaunchConfigurationRequest.class);
        verify(autoScalingClient, times(1)).deleteLaunchConfiguration(captor.capture());
        assertEquals(launchConfigurationName, captor.getValue().getLaunchConfigurationName());
        verify(resourceNotifier, times(1)).notifyDeletion(any(CloudResource.class), eq(context));
    }
}