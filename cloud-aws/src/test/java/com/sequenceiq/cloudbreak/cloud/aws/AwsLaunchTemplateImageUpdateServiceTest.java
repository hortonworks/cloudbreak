package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.github.jknack.handlebars.internal.Files;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.type.ResourceType;

public class AwsLaunchTemplateImageUpdateServiceTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String IMAGE_NAME = "imageName";

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

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @InjectMocks
    private AwsLaunchTemplateImageUpdateService underTest;

    private AuthenticatedContext ac;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Location location = Location.location(Region.region("region"));
        CloudContext cloudContext = new CloudContext(1L, "cloudContext", "AWS", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential cc = new CloudCredential("crn", "cc");
        ac = new AuthenticatedContext(cloudContext, cc);
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("GroupName");
        when(stack.getGroups()).thenReturn(List.of(group));
        when(stack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn(IMAGE_NAME);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(encryptedImageCopyService.createEncryptedImages(ac, stack, resourceNotifier)).thenReturn(Collections.emptyMap());
    }

    @Test
    public void shouldUpdateImage() throws IOException {
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name(cfStackName)
                .build();
        String template = Files.read(new File("src/test/resources/json/aws-cf-template.json"));
        String cfTemplateBody = JsonUtil.minify(String.format(template, "{\"Ref\":\"AMI\"}"));
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody(cfTemplateBody));

        UpdateStackRequest updateStackRequest = new UpdateStackRequest();
        when(awsStackRequestHelper.createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody)).thenReturn(updateStackRequest);

        underTest.updateImage(ac, stack, cfResource);

        verify(awsStackRequestHelper).createUpdateStackRequest(ac, stack, cfStackName, cfTemplateBody);
        verify(cloudFormationClient).updateStack(updateStackRequest);
    }

    @Test
    public void shouldUpdateEncryptedImage() throws IOException {
        String cfStackName = "cf";
        CloudResource cfResource = CloudResource.builder()
                .type(ResourceType.CLOUDFORMATION_STACK)
                .name(cfStackName)
                .build();
        String template = Files.read(new File("src/test/resources/json/aws-cf-template.json"));
        String cfTemplateBody = JsonUtil.minify(String.format(template, "\"ami-old\""));
        when(cloudFormationClient.getTemplate(any())).thenReturn(new GetTemplateResult().withTemplateBody(cfTemplateBody));

        UpdateStackRequest updateStackRequest = new UpdateStackRequest();
        ArgumentCaptor<String> cfTemplateBodyCaptor = ArgumentCaptor.forClass(String.class);
        when(awsStackRequestHelper.createUpdateStackRequest(eq(ac), eq(stack), eq(cfStackName), any())).thenReturn(updateStackRequest);

        underTest.updateImage(ac, stack, cfResource);

        verify(awsStackRequestHelper).createUpdateStackRequest(eq(ac), eq(stack), eq(cfStackName), cfTemplateBodyCaptor.capture());
        String expectedCfTemplate = JsonUtil.minify(String.format(template, "\"" + IMAGE_NAME + "\""));
        Assertions.assertThat(cfTemplateBodyCaptor.getValue()).isEqualTo(expectedCfTemplate);
        verify(cloudFormationClient).updateStack(updateStackRequest);
    }

}