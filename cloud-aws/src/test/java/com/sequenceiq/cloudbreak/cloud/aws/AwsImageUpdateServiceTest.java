package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public class AwsImageUpdateServiceTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private AwsClient awsClient;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private EncryptedImageCopyService encryptedImageCopyService;

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
    private Image image;

    @InjectMocks
    private AwsImageUpdateService underTest;

    private AuthenticatedContext ac;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Location location = Location.location(Region.region("region"));
        CloudContext cloudContext = new CloudContext(1L, "cloudContext", "AWS", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential cc = new CloudCredential(1L, "cc");
        ac = new AuthenticatedContext(cloudContext, cc);
        when(stack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn("imageName");
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(encryptedImageCopyService.createEncryptedImages(ac, stack, resourceNotifier)).thenReturn(Collections.emptyMap());
    }

    @Test
    public void testUpdateImage() {
        String lcName = "lcName";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name("cf")
                .build();
        String autoScalingGroupName = "autoScalingGroupName";
        Map<AutoScalingGroup, String> scalingGroupStringMap =
                Collections.singletonMap(new AutoScalingGroup().withLaunchConfigurationName(lcName)
                        .withAutoScalingGroupName(autoScalingGroupName), autoScalingGroupName);
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource))
                .thenReturn(scalingGroupStringMap);
        List<LaunchConfiguration> oldLaunchConfigs = Collections.singletonList(new LaunchConfiguration().withLaunchConfigurationName(lcName));
        when(launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet()))
                .thenReturn(oldLaunchConfigs);
        String newLCName = "newLCName";
        when(launchConfigurationHandler.createNewLaunchConfiguration(eq("imageName"), eq(autoScalingClient), eq(oldLaunchConfigs.get(0)),
                eq(ac.getCloudContext()), eq(null))).thenReturn(newLCName);

        underTest.updateImage(ac, stack, cfResource);

        verify(autoScalingGroupHandler, times(1)).getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        verify(launchConfigurationHandler, times(1)).getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet());
        verify(launchConfigurationHandler, times(1)).createNewLaunchConfiguration(anyString(), eq(autoScalingClient),
                eq(oldLaunchConfigs.get(0)), eq(ac.getCloudContext()), eq(null));
        verify(autoScalingGroupHandler, times(1)).updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient,
                autoScalingGroupName, oldLaunchConfigs.get(0), newLCName);
        verify(launchConfigurationHandler, times(1)).removeOldLaunchConfiguration(oldLaunchConfigs.get(0), autoScalingClient,
                ac.getCloudContext());
    }
}