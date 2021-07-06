package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
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

public class AwsLaunchTemplateImageUpdateServiceTest {

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

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @InjectMocks
    private AwsLaunchTemplateImageUpdateService underTest;

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
    }

    @Test
    public void shouldUpdateImage() throws IOException {
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name(cfStackName)
                .build();

        String template = FileReaderUtils.readFileFromClasspath("json/aws-cf-template.json");
        String cfTemplateBody = JsonUtil.minify(String.format(template, "{\"Ref\":\"AMI\"}"));
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody(cfTemplateBody));

        UpdateStackRequest updateStackRequest = new UpdateStackRequest();
        when(awsStackRequestHelper.createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody)).thenReturn(updateStackRequest);

        underTest.updateImage(ac, stack, cfResource);

        verify(awsStackRequestHelper).createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody);
        verify(cloudFormationClient).updateStack(updateStackRequest);
    }

    @Test
    public void testUpdateImageForValidationError() throws IOException {
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name(cfStackName)
                .build();
        String template = FileReaderUtils.readFileFromClasspath("json/aws-cf-template.json");
        String cfTemplateBody = JsonUtil.minify(String.format(template, "{\"Ref\":\"AMI\"}"));
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody(cfTemplateBody));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cloudFormationClient);

        UpdateStackRequest updateStackRequest = new UpdateStackRequest();
        when(awsStackRequestHelper.createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody)).thenReturn(updateStackRequest);
        AmazonCloudFormationException exception = new AmazonCloudFormationException("Cannot execute method: updateStack. No updates are to be performed.");
        exception.setErrorCode("ValidationError");
        when(cloudFormationClient.updateStack(updateStackRequest)).thenThrow(exception);

        underTest.updateImage(ac, stack, cfResource);

        verify(awsStackRequestHelper).createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody);
        verify(cloudFormationClient).updateStack(updateStackRequest);
    }

    @Test
    public void testUpdateImageForValidationErrorShouldThrow() throws IOException {
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name(cfStackName)
                .build();
        String template = FileReaderUtils.readFileFromClasspath("json/aws-cf-template.json");
        String cfTemplateBody = JsonUtil.minify(String.format(template, "{\"Ref\":\"AMI\"}"));
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody(cfTemplateBody));
        when(awsClient.createCloudFormationClient(any(), any())).thenReturn(cloudFormationClient);

        UpdateStackRequest updateStackRequest = new UpdateStackRequest();
        when(awsStackRequestHelper.createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody)).thenReturn(updateStackRequest);
        AmazonCloudFormationException exception = new AmazonCloudFormationException("Some other message");
        exception.setErrorCode("NotValidationError");
        when(cloudFormationClient.updateStack(updateStackRequest)).thenThrow(exception);

        try {
            underTest.updateImage(ac, stack, cfResource);
        } catch (AmazonCloudFormationException e) {
            assertEquals(e.getErrorMessage(), "Some other message");
            assertEquals(e.getErrorCode(), "NotValidationError");
        }

        verify(awsStackRequestHelper).createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody);
        verify(cloudFormationClient).updateStack(updateStackRequest);
    }

}