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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

public class AwsLaunchConfigurationImageUpdateServiceTest {

    private static final String USER_ID = "horton@hortonworks.com";

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
    private Image image;

    @InjectMocks
    private AwsLaunchConfigurationImageUpdateService underTest;

    private AuthenticatedContext ac;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Location location = Location.location(Region.region("region"));
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("variant")
                .withLocation(location)
                .withUserId(USER_ID)
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cc = new CloudCredential("crn", "cc");
        ac = new AuthenticatedContext(context, cc);
        when(stack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn(IMAGE_NAME);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
    }

    @Test
    public void shouldUpdateImage() {
        String lcName = "lcName";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name("cf")
                .build();
        String autoScalingGroupName = "autoScalingGroupName";
        Map<AutoScalingGroup, String> scalingGroupStringMap =
                Collections.singletonMap(new AutoScalingGroup().withLaunchConfigurationName(lcName)
                        .withAutoScalingGroupName(autoScalingGroupName), autoScalingGroupName);
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody("AWS::AutoScaling::LaunchConfiguration"));
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource))
                .thenReturn(scalingGroupStringMap);
        List<LaunchConfiguration> oldLaunchConfigs = Collections.singletonList(new LaunchConfiguration().withLaunchConfigurationName(lcName));
        when(launchConfigurationHandler.getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet()))
                .thenReturn(oldLaunchConfigs);
        String newLCName = "newLCName";
        when(launchConfigurationHandler.createNewLaunchConfiguration(eq("imageName"), eq(autoScalingClient), eq(oldLaunchConfigs.get(0)),
                eq(ac.getCloudContext()))).thenReturn(newLCName);

        underTest.updateImage(ac, stack, cfResource);

        verify(autoScalingGroupHandler, times(1)).getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource);
        verify(launchConfigurationHandler, times(1)).getLaunchConfigurations(autoScalingClient, scalingGroupStringMap.keySet());
        verify(launchConfigurationHandler, times(1)).createNewLaunchConfiguration(anyString(), eq(autoScalingClient),
                eq(oldLaunchConfigs.get(0)), eq(ac.getCloudContext()));
        verify(autoScalingGroupHandler, times(1)).updateAutoScalingGroupWithLaunchConfiguration(autoScalingClient,
                autoScalingGroupName, oldLaunchConfigs.get(0), newLCName);
        verify(launchConfigurationHandler, times(1)).removeOldLaunchConfiguration(oldLaunchConfigs.get(0), autoScalingClient,
                ac.getCloudContext());
    }
}