package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@ExtendWith(MockitoExtension.class)
public class AwsImageUpdateServiceTest {

    private static final String IMAGE_NAME = "imagename";

    @Mock
    private CloudStack stack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudResource cfResource;

    @Mock
    private AwsLaunchConfigurationUpdateService awsLaunchConfigurationUpdateService;

    @Mock
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    @InjectMocks
    private AwsImageUpdateService underTest;

    @Test
    void shouldUpdateImageInLaunchConfiguration() {
        when(stack.getTemplate()).thenReturn("AWS::AutoScaling::LaunchConfiguration");
        Image image = mock(Image.class);
        when(image.getImageName()).thenReturn(IMAGE_NAME);
        when(stack.getImage()).thenReturn(image);

        underTest.updateImage(ac, stack, cfResource);

        String cfName = cfResource.getName();
        verify(awsLaunchConfigurationUpdateService).updateLaunchConfigurations(ac, stack, cfResource, Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME));
        verify(awsLaunchTemplateUpdateService, never()).updateFieldsOnAllLaunchTemplate(eq(ac), eq(cfName), anyMap(), eq(stack));
    }

    @Test
    void shouldUpdateImageInLaunchTemplate() {
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");
        when(stack.getImage()).thenReturn(new Image(IMAGE_NAME, null, null, null, null, null, null, "imageid", null, null, null, null));

        underTest.updateImage(ac, stack, cfResource);

        String cfName = cfResource.getName();
        verify(awsLaunchTemplateUpdateService).updateFieldsOnAllLaunchTemplate(eq(ac), eq(cfName),
                eq(Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME, LaunchTemplateField.ROOT_DISK_PATH, "")), eq(stack));
        verify(awsLaunchConfigurationUpdateService, never()).updateLaunchConfigurations(ac, stack, cfResource, Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME));
    }

    @Test
    void shouldThrowExceptionWhenTemplateDoesNotMatchNeither() {
        when(stack.getTemplate()).thenReturn("gibberish");

        Assertions.assertThrows(NotImplementedException.class, () -> underTest.updateImage(ac, stack, cfResource));

        String cfName = cfResource.getName();
        verify(awsLaunchTemplateUpdateService, never()).updateFieldsOnAllLaunchTemplate(eq(ac), eq(cfName), anyMap(), eq(stack));
        verify(awsLaunchConfigurationUpdateService, never())
                .updateLaunchConfigurations(ac, stack, cfResource, Map.of(LaunchTemplateField.IMAGE_ID, IMAGE_NAME));
    }
}
