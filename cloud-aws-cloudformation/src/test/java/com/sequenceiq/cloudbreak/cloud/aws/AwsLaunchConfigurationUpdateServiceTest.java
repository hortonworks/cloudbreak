package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;

@ExtendWith(MockitoExtension.class)
public class AwsLaunchConfigurationUpdateServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String IMAGE_NAME = "imageName";

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private LaunchConfigurationHandler launchConfigurationHandler;

    @Mock
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @Mock
    private AmazonCloudFormationClient cloudFormationClient;

    @Mock
    private CloudStack stack;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private InstanceInAutoScalingGroupUpdater instanceUpdater;

    @InjectMocks
    private AwsLaunchConfigurationUpdateService underTest;

    private AuthenticatedContext ac;

    @BeforeEach
    public void setup() {
        Location location = Location.location(Region.region("region"));
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("variant")
                .withLocation(location)
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cc = new CloudCredential("crn", "cc", "account");
        ac = new AuthenticatedContext(context, cc);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
    }

    @Test
    public void shouldUpdateImage() {
        String lcName = "lcName";
        CloudResource cfResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName("cf")
                .build();
        String autoScalingGroupName = "autoScalingGroupName";
        Map<AutoScalingGroup, String> scalingGroupStringMap =
                Collections.singletonMap(AutoScalingGroup.builder().launchConfigurationName(lcName)
                        .autoScalingGroupName(autoScalingGroupName).build(), autoScalingGroupName);
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource))
                .thenReturn(scalingGroupStringMap);
        List<LaunchConfiguration> oldLaunchConfigs = Collections.singletonList(LaunchConfiguration.builder().launchConfigurationName(lcName).build());
        when(launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet()))
                .thenReturn(oldLaunchConfigs);
        String newLCName = "newLCName";
        when(launchConfigurationHandler.createNewLaunchConfiguration(eq(Map.of(LaunchTemplateField.IMAGE_ID, "imageName")), eq(autoScalingClient),
                eq(oldLaunchConfigs.get(0)), eq(ac.getCloudContext()), eq(ac), eq(stack))).thenReturn(newLCName);

        underTest.updateLaunchConfigurations(ac, stack, cfResource, Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME), false);

        verify(autoScalingGroupHandler, times(1)).getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        verify(launchConfigurationHandler, times(1)).getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet());
        verify(launchConfigurationHandler, times(1)).createNewLaunchConfiguration(eq(Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME)),
                eq(autoScalingClient), eq(oldLaunchConfigs.get(0)), eq(ac.getCloudContext()), eq(ac), eq(stack));
        verify(autoScalingGroupHandler, times(1)).updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient,
                autoScalingGroupName, newLCName);
        verify(launchConfigurationHandler, times(1)).removeOldLaunchConfiguration(oldLaunchConfigs.get(0), autoScalingClient,
                ac.getCloudContext());
    }

    @Test
    public void testUpdateInstances() {
        String lcName = "lcName";
        CloudResource cfResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName("cf")
                .build();
        String autoScalingGroupName = "autoScalingGroupName";
        AutoScalingGroup autoScalingGroup = AutoScalingGroup.builder().launchConfigurationName(lcName)
                .autoScalingGroupName(autoScalingGroupName).build();
        Map<AutoScalingGroup, String> scalingGroupStringMap =
                Collections.singletonMap(autoScalingGroup, autoScalingGroupName);
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource))
                .thenReturn(scalingGroupStringMap);
        List<LaunchConfiguration> oldLaunchConfigs = Collections.singletonList(LaunchConfiguration.builder().launchConfigurationName(lcName).build());
        when(launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet()))
                .thenReturn(oldLaunchConfigs);
        String newLCName = "newLCName";
        when(launchConfigurationHandler.createNewLaunchConfiguration(eq(Map.of(LaunchTemplateField.IMAGE_ID, "imageName")), eq(autoScalingClient),
                eq(oldLaunchConfigs.get(0)), eq(ac.getCloudContext()), eq(ac), eq(stack))).thenReturn(newLCName);
        Group group = mock(Group.class);
        String groupName = "groupName";
        when(group.getName()).thenReturn(groupName);
        when(cfStackUtil.getAutoscalingGroupName(ac, groupName, "region")).thenReturn(autoScalingGroupName);
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("region"))).thenReturn(ec2Client);

        underTest.updateLaunchConfigurations(ac, stack, cfResource, Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME), group, false);

        verify(autoScalingGroupHandler, times(1)).getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        verify(launchConfigurationHandler, times(1)).getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet());
        verify(launchConfigurationHandler, times(1)).createNewLaunchConfiguration(eq(Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME)),
                eq(autoScalingClient), eq(oldLaunchConfigs.get(0)), eq(ac.getCloudContext()), eq(ac), eq(stack));
        verify(autoScalingGroupHandler, times(1)).updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient,
                autoScalingGroupName, newLCName);
        verify(launchConfigurationHandler, times(1)).removeOldLaunchConfiguration(oldLaunchConfigs.get(0), autoScalingClient,
                ac.getCloudContext());
        verify(instanceUpdater).updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group);
        verifyNoMoreInteractions(instanceUpdater);
    }
}
