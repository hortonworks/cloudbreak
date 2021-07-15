package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@ExtendWith(MockitoExtension.class)
public class AwsImageUpdateServiceTest {

    @Mock
    private CloudStack stack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudResource cfResource;

    @Mock
    private AwsLaunchConfigurationImageUpdateService awsLaunchConfigurationImageUpdateService;

    @Mock
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    @InjectMocks
    private AwsImageUpdateService underTest;

    @Test
    void shouldUpdateImageInLaunchConfiguration() {
        Mockito.when(stack.getTemplate()).thenReturn("AWS::AutoScaling::LaunchConfiguration");

        underTest.updateImage(ac, stack, cfResource);

        verify(awsLaunchConfigurationImageUpdateService).updateImage(ac, stack, cfResource);
        verify(awsLaunchTemplateUpdateService, never()).updateFields(ac, cfResource, anyMap());
    }

    @Test
    void shouldUpdateImageInLaunchTemplate() {
        Mockito.when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");

        underTest.updateImage(ac, stack, cfResource);

        verify(awsLaunchTemplateUpdateService).updateFields(ac, cfResource, anyMap());
        verify(awsLaunchConfigurationImageUpdateService, never()).updateImage(ac, stack, cfResource);
    }

    @Test
    void shouldThrowExceptionWhenTemplateDoesNotMatchNeither() {
        Mockito.when(stack.getTemplate()).thenReturn("gibberish");

        Assertions.assertThrows(NotImplementedException.class, () -> underTest.updateImage(ac, stack, cfResource));

        verify(awsLaunchTemplateUpdateService, never()).updateFields(ac, cfResource, anyMap());
        verify(awsLaunchConfigurationImageUpdateService, never()).updateImage(ac, stack, cfResource);
    }
}
