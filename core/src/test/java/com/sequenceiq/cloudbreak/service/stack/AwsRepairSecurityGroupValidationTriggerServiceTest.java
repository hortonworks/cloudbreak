package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class AwsRepairSecurityGroupValidationTriggerServiceTest {

    @Mock
    private StackDto stackDto;

    @InjectMocks
    private AwsRepairSecurityGroupValidationTriggerService underTest;

    @ParameterizedTest
    @ValueSource(strings = {"AWS", "AWS_NATIVE", "AWS_NATIVE_GOV"})
    void shouldRunForAwsPlatformAndSupportedVariants(String platformVariant) {
        whenAwsStack(platformVariant);

        assertTrue(underTest.shouldRunSecurityGroupValidation(stackDto));
    }

    @Test
    void shouldNotRunForNonAwsPlatform() {
        when(stackDto.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());

        assertFalse(underTest.shouldRunSecurityGroupValidation(stackDto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AZURE", "GCP", "UNKNOWN"})
    void shouldNotRunForUnsupportedVariant(String platformVariant) {
        whenAwsStack(platformVariant);

        assertFalse(underTest.shouldRunSecurityGroupValidation(stackDto));
    }

    private void whenAwsStack(String platformVariant) {
        when(stackDto.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(stackDto.getPlatformVariant()).thenReturn(platformVariant);
    }
}
