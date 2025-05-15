package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.client.thunderhead.computeapi.ThunderheadComputeApiService;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.dataservices.AzureDataServiceParameters;
import com.sequenceiq.environment.environment.dto.dataservices.CustomDockerRegistryParameters;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;

@ExtendWith(MockitoExtension.class)
class EnvironmentDataServicesValidatorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentValidationDto environmentValidationDto;

    @Mock
    private ThunderheadComputeApiService thunderheadComputeApiService;

    @Mock
    private ManagedIdentityRoleValidator managedIdentityRoleValidator;

    @InjectMocks
    private EnvironmentDataServicesValidator underTest;

    @Test
    void validateWhenNoDataServicesSpecifiedOnTheEnvironment() {
        when(environmentValidationDto.getEnvironmentDto().getDataServices()).thenReturn(null);

        ValidationResult validationResult = underTest.validate(environmentValidationDto);

        assertFalse(validationResult.hasError());
        verifyNoInteractions(thunderheadComputeApiService);
    }

    @Test
    void validateWhenNoCustomDockerRegistrySpecifiedOnDataServicesOnTheEnvironment() {
        when(environmentValidationDto.getEnvironmentDto().getDataServices().customDockerRegistry()).thenReturn(null);

        ValidationResult validationResult = underTest.validate(environmentValidationDto);

        assertFalse(validationResult.hasError());
        verifyNoInteractions(thunderheadComputeApiService);
    }

    @Test
    void validateWhenManagedIdentityDefinedAndWrongFormatOfIdentity() {
        AzureDataServiceParameters azureDataServiceParameters = new AzureDataServiceParameters("test");
        EnvironmentDataServices environmentDataServices = EnvironmentDataServices.builder()
                .withAzure(azureDataServiceParameters)
                .build();

        when(environmentValidationDto.getEnvironmentDto().getDataServices()).thenReturn(environmentDataServices);
        when(managedIdentityRoleValidator.validateEncryptionRole(anyString())).thenReturn(
                ValidationResult.builder()
                        .error("Error")
                        .build());

        ValidationResult validationResult = underTest.validate(environmentValidationDto);

        assertTrue(validationResult.hasError());
        verifyNoInteractions(thunderheadComputeApiService);
    }

    @Test
    void validateWhenManagedIdentityDefinedAndRightFormatOfIdentity() {
        AzureDataServiceParameters azureDataServiceParameters = new AzureDataServiceParameters("test");
        EnvironmentDataServices environmentDataServices = EnvironmentDataServices.builder()
                .withAzure(azureDataServiceParameters)
                .build();

        when(environmentValidationDto.getEnvironmentDto().getDataServices()).thenReturn(environmentDataServices);
        when(managedIdentityRoleValidator.validateEncryptionRole(anyString())).thenReturn(
                ValidationResult.builder()
                        .build());

        ValidationResult validationResult = underTest.validate(environmentValidationDto);

        assertFalse(validationResult.hasError());
        verifyNoInteractions(thunderheadComputeApiService);
    }

    @Test
    void validateWhenCustomDockerRegistrySpecifiedOnDataServicesOnTheEnvironmentAndCouldBeDescribed() {
        CustomDockerRegistryParameters customDockerRegistryParameters = new CustomDockerRegistryParameters("dummyCrn");
        when(environmentValidationDto.getEnvironmentDto().getDataServices().customDockerRegistry()).thenReturn(customDockerRegistryParameters);
        when(thunderheadComputeApiService.customConfigDescribable(customDockerRegistryParameters)).thenReturn(Boolean.TRUE);

        ValidationResult validationResult = underTest.validate(environmentValidationDto);

        assertFalse(validationResult.hasError());
        verify(thunderheadComputeApiService).customConfigDescribable(customDockerRegistryParameters);
    }

    @Test
    void validateWhenCustomDockerRegistrySpecifiedOnDataServicesOnTheEnvironmentAndCouldNotBeDescribed() {
        CustomDockerRegistryParameters customDockerRegistryParameters = new CustomDockerRegistryParameters("dummyCrn");
        when(environmentValidationDto.getEnvironmentDto().getDataServices().customDockerRegistry()).thenReturn(customDockerRegistryParameters);
        when(thunderheadComputeApiService.customConfigDescribable(customDockerRegistryParameters)).thenReturn(Boolean.FALSE);

        ValidationResult validationResult = underTest.validate(environmentValidationDto);

        assertTrue(validationResult.hasError());
        assertEquals("The validation of the specified custom docker registry config with CRN('dummyCrn') failed on the Compute API",
                validationResult.getFormattedErrors());
        verify(thunderheadComputeApiService).customConfigDescribable(customDockerRegistryParameters);
    }
}