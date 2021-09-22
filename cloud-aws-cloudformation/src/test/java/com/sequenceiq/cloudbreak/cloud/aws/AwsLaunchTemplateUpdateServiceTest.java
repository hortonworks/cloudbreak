package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchTemplate;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.amazonaws.services.autoscaling.model.MixedInstancesPolicy;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionResult;
import com.amazonaws.services.ec2.model.LaunchTemplateVersion;
import com.amazonaws.services.ec2.model.ModifyLaunchTemplateRequest;
import com.amazonaws.services.ec2.model.ModifyLaunchTemplateResult;
import com.amazonaws.services.ec2.model.ValidationError;
import com.amazonaws.services.ec2.model.ValidationWarning;
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
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.ResourceType;

public class AwsLaunchTemplateUpdateServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String IMAGE_NAME = "imageName";

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

    private AuthenticatedContext ac;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Location location = Location.location(Region.region("region"));
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("variant")
                .withLocation(location)
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cc = new CloudCredential("crn", "cc");
        ac = new AuthenticatedContext(context, cc);
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("GroupName");
        when(stack.getGroups()).thenReturn(List.of(group));
        when(stack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn(IMAGE_NAME);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(awsClient.createEc2Client(any(AwsCredentialView.class), anyString())).thenReturn(ec2Client);
    }

    @Test
    public void shouldUpdateImage() throws IOException {
        // GIVEN
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name(cfStackName)
                .build();

        String template = FileReaderUtils.readFileFromClasspath("json/aws-cf-template.json");
        String cfTemplateBody = JsonUtil.minify(String.format(template, "{\"Ref\":\"AMI\"}"));
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody(cfTemplateBody));

        Map<AutoScalingGroup, String> autoScalingGroupsResult = createAutoScalingGroupHandler();
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource)).thenReturn(autoScalingGroupsResult);
        when(ec2Client.createLaunchTemplateVersion(any(CreateLaunchTemplateVersionRequest.class))).thenReturn(new CreateLaunchTemplateVersionResult()
                .withLaunchTemplateVersion(new LaunchTemplateVersion().withVersionNumber(1L)));
        when(ec2Client.modifyLaunchTemplate(any(ModifyLaunchTemplateRequest.class))).thenReturn(new ModifyLaunchTemplateResult());
        when(autoScalingClient.updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class))).thenReturn(new UpdateAutoScalingGroupResult());

        // WHEN
        underTest.updateFields(ac, cfResource.getName(), Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName()));

        // THEN no exception
    }

    @Test
    public void testUpdateImageWithWrongTemplateParams() throws IOException {
        // GIVEN
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name(cfStackName)
                .build();

        String template = FileReaderUtils.readFileFromClasspath("json/aws-cf-template.json");
        String cfTemplateBody = JsonUtil.minify(String.format(template, "{\"Ref\":\"AMI\"}"));
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody(cfTemplateBody));

        Map<AutoScalingGroup, String> autoScalingGroupsResult = createAutoScalingGroupHandler();
        when(autoScalingGroupHandler.getAutoScalingGroups(cloudFormationClient, autoScalingClient, cfResource.getName())).thenReturn(autoScalingGroupsResult);
        when(ec2Client.createLaunchTemplateVersion(any(CreateLaunchTemplateVersionRequest.class))).thenReturn(new CreateLaunchTemplateVersionResult()
                .withWarning(new ValidationWarning().withErrors(new ValidationError().withCode("1").withMessage("error"))));

        // WHEN and THEN exception
        Assert.assertThrows(CloudConnectorException.class,
                () -> underTest.updateFields(ac, cfResource.getName(), Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName())));
    }

    private Map<AutoScalingGroup, String> createAutoScalingGroupHandler() {
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup().withAutoScalingGroupName("ag").withMixedInstancesPolicy(createMixedInstancePolicy());
        return Map.of(autoScalingGroup, autoScalingGroup.getAutoScalingGroupName());
    }

    private MixedInstancesPolicy createMixedInstancePolicy() {
        return new MixedInstancesPolicy().withLaunchTemplate(new LaunchTemplate().withLaunchTemplateSpecification(createLaunchTemplateSpecification()));
    }

    private LaunchTemplateSpecification createLaunchTemplateSpecification() {
        return new LaunchTemplateSpecification().withLaunchTemplateId("templateid").withVersion("1");
    }
}
