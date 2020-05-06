package com.sequenceiq.environment.parameters.validation.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.featureswitch.AzureSingleResourceGroupFeatureSwitch;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto.Builder;
import com.sequenceiq.environment.parameters.validation.validators.parameter.AzureParameterValidator;

public class AzureParameterValidatorTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private AzureSingleResourceGroupFeatureSwitch azureSingleResourceGroupFeatureSwitch;

    @InjectMocks
    private AzureParameterValidator underTest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(azureSingleResourceGroupFeatureSwitch.isActive()).thenReturn(true);
    }

    @Test
    public void testWhenNoAzureParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder().build();

        ValidationResult validationResult = underTest.validate(environmentDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenNoResourceGroupThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder().build())
                .build();

        ValidationResult validationResult = underTest.validate(environmentDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenUseMultipleResourceGroupsThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .build())
                .build();

        ValidationResult validationResult = underTest.validate(environmentDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenCreateNewResourceGroupThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.CREATE_NEW).build())
                        .build())
                .build();

        ValidationResult validationResult = underTest.validate(environmentDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenUseExistingResourceGroupAndExistsThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName("myResourceGroup").build())
                        .build())
                .build();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists("myResourceGroup")).thenReturn(true);

        ValidationResult validationResult = underTest.validate(environmentDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenUseExistingResourceGroupAndNotExistsThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName("myResourceGroup").build())
                        .build())
                .build();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists("myResourceGroup")).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals("1. Resource group 'myResourceGroup' does not exist.", validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenFeatureTurnedOffThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName("myResourceGroup").build())
                        .build())
                .build();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists("myResourceGroup")).thenReturn(false);
        when(azureSingleResourceGroupFeatureSwitch.isActive()).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(azureSingleResourceGroupFeatureSwitch, times(2)).isActive();
    }

    @Test
    public void testCloudPlatform() {
        assertEquals(CloudPlatform.AZURE, underTest.getcloudPlatform());
    }

    private static class EnvironmentDtoBuilder {
        private final EnvironmentDto environmentDto = new EnvironmentDto();

        private final Builder parametersDtoBuilder = ParametersDto.builder();

        public EnvironmentDtoBuilder withAzureParameters(AzureParametersDto azureParametersDto) {
            parametersDtoBuilder.withAzureParameters(azureParametersDto);
            return this;
        }

        public EnvironmentDto build() {
            ParametersDto parametersDto = parametersDtoBuilder.build();
            environmentDto.setParameters(parametersDto);
            return environmentDto;
        }
    }
}
