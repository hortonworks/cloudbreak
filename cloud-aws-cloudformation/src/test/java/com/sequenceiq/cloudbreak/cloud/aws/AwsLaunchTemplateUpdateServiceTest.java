package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplate;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.autoscaling.model.MixedInstancesPolicy;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionRequest;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionResponse;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.ValidationError;
import software.amazon.awssdk.services.ec2.model.ValidationWarning;

@ExtendWith(MockitoExtension.class)
public class AwsLaunchTemplateUpdateServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String IMAGE_NAME = "imageName";

    private static final String DESCRIPTION = "description";

    private static final String USER_DATA = Base64.getEncoder().encodeToString("userdata".getBytes());

    private static final long LAUNCH_TEMPLATE_VERSION = 1L;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @Mock
    private AmazonCloudFormationClient cloudFormationClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private CloudStack stack;

    @Mock
    private Image image;

    @InjectMocks
    private AwsLaunchTemplateUpdateService underTest;

    @Captor
    private ArgumentCaptor<CreateLaunchTemplateVersionRequest> argumentCaptor;

    private AuthenticatedContext ac;

    @BeforeEach
    public void setup() {
        Location location = Location.location(Region.region("region"));
        CloudContext context = CloudContext.Builder.builder()
                .withId(LAUNCH_TEMPLATE_VERSION)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("variant")
                .withLocation(location)
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cc = new CloudCredential("crn", "cc", "account");
        ac = new AuthenticatedContext(context, cc);
        lenient().when(stack.getImage()).thenReturn(image);
        lenient().when(image.getImageName()).thenReturn(IMAGE_NAME);
        lenient().when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        lenient().when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        lenient().when(awsClient.createEc2Client(any(AwsCredentialView.class), anyString())).thenReturn(ec2Client);
    }

    @Test
    public void shouldUpdateImage() {
        // GIVEN
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName(cfStackName)
                .build();

        Map<AutoScalingGroup, String> autoScalingGroupsResult = createAutoScalingGroupHandler();
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource.getName())).thenReturn(autoScalingGroupsResult);
        when(ec2Client.createLaunchTemplateVersion(any(CreateLaunchTemplateVersionRequest.class))).thenReturn(CreateLaunchTemplateVersionResponse.builder()
                .launchTemplateVersion(LaunchTemplateVersion.builder().versionNumber(LAUNCH_TEMPLATE_VERSION).build()).build());
        when(ec2Client.modifyLaunchTemplate(any(ModifyLaunchTemplateRequest.class))).thenReturn(ModifyLaunchTemplateResponse.builder().build());
        when(autoScalingClient.updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class))).thenReturn(UpdateAutoScalingGroupResponse.builder().build());

        // WHEN
        underTest.updateFieldsOnAllLaunchTemplate(ac, cfResource.getName(), Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName()));

        // THEN
        verify(ec2Client).createLaunchTemplateVersion(argumentCaptor.capture());
        CreateLaunchTemplateVersionRequest request = argumentCaptor.getValue();
        assertEquals(stack.getImage().getImageName(), request.launchTemplateData().imageId());
    }

    @Test
    public void testUpdateFieldsUpdatesTheAppropriateParams() {
        // GIVEN
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName(cfStackName)
                .build();

        Map<AutoScalingGroup, String> autoScalingGroupsResult = createAutoScalingGroupHandler();
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource.getName())).thenReturn(autoScalingGroupsResult);
        Map<LaunchTemplateField, String> updatableFieldMap = Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName(),
                LaunchTemplateField.DESCRIPTION, DESCRIPTION, LaunchTemplateField.USER_DATA, USER_DATA);
        when(ec2Client.createLaunchTemplateVersion(any(CreateLaunchTemplateVersionRequest.class))).thenReturn(CreateLaunchTemplateVersionResponse.builder()
                .launchTemplateVersion(LaunchTemplateVersion.builder().versionNumber(LAUNCH_TEMPLATE_VERSION).build()).build());
        // WHEN
        underTest.updateFieldsOnAllLaunchTemplate(ac, cfResource.getName(), updatableFieldMap);

        // THEN
        verify(ec2Client).createLaunchTemplateVersion(argumentCaptor.capture());
        CreateLaunchTemplateVersionRequest request = argumentCaptor.getValue();
        assertEquals(stack.getImage().getImageName(), request.launchTemplateData().imageId());
        assertEquals(USER_DATA, request.launchTemplateData().userData());
        assertEquals(DESCRIPTION, request.versionDescription());
    }

    @Test
    public void testUpdateFieldsUpdatesWithMissingParams() {
        // GIVEN
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName(cfStackName)
                .build();

        Map<AutoScalingGroup, String> autoScalingGroupsResult = createAutoScalingGroupHandler();
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource.getName())).thenReturn(autoScalingGroupsResult);
        Map<LaunchTemplateField, String> updatableFieldMap = Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName(),
                LaunchTemplateField.DESCRIPTION, DESCRIPTION);
        when(ec2Client.createLaunchTemplateVersion(any(CreateLaunchTemplateVersionRequest.class))).thenReturn(CreateLaunchTemplateVersionResponse.builder()
                .launchTemplateVersion(LaunchTemplateVersion.builder().versionNumber(LAUNCH_TEMPLATE_VERSION).build()).build());
        // WHEN
        underTest.updateFieldsOnAllLaunchTemplate(ac, cfResource.getName(), updatableFieldMap);

        // THEN
        verify(ec2Client).createLaunchTemplateVersion(argumentCaptor.capture());
        CreateLaunchTemplateVersionRequest request = argumentCaptor.getValue();
        assertEquals(stack.getImage().getImageName(), request.launchTemplateData().imageId());
        assertEquals(DESCRIPTION, request.versionDescription());
        assertNull(request.launchTemplateData().userData());
    }

    @Test
    public void testUpdateLaunchTemplate() {
        // GIVEN
        Map<LaunchTemplateField, String> updatableFields = new HashMap<>();
        AmazonAutoScalingClient autoScalingClient = mock(AmazonAutoScalingClient.class);
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        LaunchTemplateSpecification launchTemplateSpecification = LaunchTemplateSpecification.builder().launchTemplateId("1").launchTemplateName("lt").build();
        LaunchTemplateVersion launchTemplateVersion = LaunchTemplateVersion.builder().versionNumber(LAUNCH_TEMPLATE_VERSION).build();
        AutoScalingGroup asgEntry = AutoScalingGroup.builder().autoScalingGroupName("master").launchTemplate(launchTemplateSpecification).build();
        CreateLaunchTemplateVersionResponse createLaunchTemplateVersionResult = CreateLaunchTemplateVersionResponse.builder()
                .launchTemplateVersion(launchTemplateVersion)
                .build();
        ModifyLaunchTemplateResponse modifyLaunchTemplateResult = ModifyLaunchTemplateResponse.builder().build();
        UpdateAutoScalingGroupResponse updateAutoScalingGroupResult = UpdateAutoScalingGroupResponse.builder().build();

        when(ec2Client.createLaunchTemplateVersion(any())).thenReturn(createLaunchTemplateVersionResult);
        when(ec2Client.modifyLaunchTemplate(any())).thenReturn(modifyLaunchTemplateResult);
        when(autoScalingClient.updateAutoScalingGroup(any())).thenReturn(updateAutoScalingGroupResult);
        // WHEN
        underTest.updateLaunchTemplate(updatableFields, false, autoScalingClient, ec2Client, asgEntry);

        // THEN
        verify(ec2Client, times(1)).createLaunchTemplateVersion(any());
        verify(ec2Client, times(1)).modifyLaunchTemplate(any());
        verify(autoScalingClient, times(1)).updateAutoScalingGroup(any());
    }

    @Test
    public void testUpdateImageWithWrongTemplateParams() {
        // GIVEN
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName(cfStackName)
                .build();

        Map<AutoScalingGroup, String> autoScalingGroupsResult = createAutoScalingGroupHandler();
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource.getName())).thenReturn(autoScalingGroupsResult);
        when(ec2Client.createLaunchTemplateVersion(any(CreateLaunchTemplateVersionRequest.class)))
                .thenReturn(CreateLaunchTemplateVersionResponse.builder()
                        .warning(ValidationWarning.builder().errors(ValidationError.builder().code("1").message("error").build()).build()).build());

        // WHEN and THEN exception
        assertThrows(CloudConnectorException.class,
                () -> underTest.updateFieldsOnAllLaunchTemplate(ac, cfResource.getName(),
                        Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName())));
    }

    private Map<AutoScalingGroup, String> createAutoScalingGroupHandler() {
        AutoScalingGroup autoScalingGroup = AutoScalingGroup.builder().autoScalingGroupName("ag").mixedInstancesPolicy(createMixedInstancePolicy()).build();
        return Map.of(autoScalingGroup, autoScalingGroup.autoScalingGroupName());
    }

    private MixedInstancesPolicy createMixedInstancePolicy() {
        return MixedInstancesPolicy.builder()
                .launchTemplate(LaunchTemplate.builder()
                        .launchTemplateSpecification(createLaunchTemplateSpecification()).build())
                .build();
    }

    private LaunchTemplateSpecification createLaunchTemplateSpecification() {
        return LaunchTemplateSpecification.builder().launchTemplateId("templateid").version("1").build();
    }
}
