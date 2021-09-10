package com.sequenceiq.environment.parameters.validation.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto.Builder;
import com.sequenceiq.environment.parameter.dto.ResourceGroupCreation;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameters.validation.validators.parameter.AzureParameterValidator;

public class AzureParameterValidatorTest {

    private static final String KEY_URL = "dummyKeyUrl";

    private static final String RESOURCE_GROUP_NAME = "myResourceGroup";

    private static final String ENCRYPTION_KEY_RESOURCE_GROUP_NAME = "dummyEncryptionKeyResourceGroup";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AzureParameterValidator underTest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(entitlementService.azureSingleResourceGroupDeploymentEnabled(anyString())).thenReturn(true);
        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(true);
    }

    @Test
    public void testWhenNoAzureParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder().build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenNoResourceGroupThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder().build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenUseMultipleResourceGroupsAndNoEncryptionParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenUseExistingResourceGroupAndExistsAndNoEncryptionParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName(RESOURCE_GROUP_NAME).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenMultipleResourceGroupAndEmptyNameAndNoEncryptionParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenMultipleResourceGroupAndNonEmptyNameAndNoEncryptionParametersThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName(RESOURCE_GROUP_NAME).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(String.format("You specified to use multiple resource groups for your resources, " +
                        "but then the single resource group name '%s' cannot not be specified.", RESOURCE_GROUP_NAME),
                validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenMultipleResourceGroupAndEncryptionKeyUrlAndNoEncryptionKeyResourceGroupNameThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyUrl("dummyKeyUrl")
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(String.format("To use Server Side Encryption for Azure Managed disks with CMK, at least one of --encryption-key-resource-group-name or " +
                        "--resource-group-name should be specified. Please provide --resource-group-name, if encryption key is present in the same " +
                        "resource group you wish to create the environment in, or provide --encryption-key-resource-group-name."),
                validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenUseExistingResourceGroupAndEmptyNameAndNoEncryptionParametersThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName("").build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals("If you use a single resource group for your resources then please provide the name of that resource group.",
                validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenUseExistingResourceGroupAndEmptyResourceGroupUsageAndNoEncryptionParametersThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName(RESOURCE_GROUP_NAME).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals("If you have provided the resource group name for your resources then please provide the resource group usage pattern too.",
                validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenUseExistingResourceGroupAndNotExistsAndNoEncryptionParametersThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName(RESOURCE_GROUP_NAME).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(String.format("Resource group '%s' does not exist or insufficient permission to access it.", RESOURCE_GROUP_NAME),
                validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenFeatureTurnedOffAndUseMultipleAndNoEncryptionParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(false);
        when(entitlementService.azureSingleResourceGroupDeploymentEnabled(anyString())).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(azureClientService, never()).getClient(any());
        verify(entitlementService, times(0)).azureSingleResourceGroupDeploymentEnabled(anyString());
    }

    @Test
    public void testWhenFeatureTurnedOffAndUseSingleAndNoEncryptionParametersThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName(RESOURCE_GROUP_NAME).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(false);
        when(entitlementService.azureSingleResourceGroupDeploymentEnabled(anyString())).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals("You specified to use a single resource group for all of your resources, but that feature is currently disabled",
                validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenDedicatedStorageAccountFeatureTurnedOffAndUseSingleAndNoEncryptionParametersThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT)
                                .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                                .withName(RESOURCE_GROUP_NAME).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        AzureClient azureClient = mock(AzureClient.class);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(false);
        when(entitlementService.azureSingleResourceGroupDeploymentEnabled(anyString())).thenReturn(true);
        when(entitlementService.azureSingleResourceGroupDedicatedStorageAccountEnabled(anyString())).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals("You specified to use a single resource group with dedicated storage account for the images, but that feature is currently disabled",
                validationResult.getFormattedErrors());
    }

    @Test
    public void testWhenUseMultipleResourceGroupsAndResourceEncryptionParameterKeyUrlAndEntitlementDisabledThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyUrl(KEY_URL).build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(false);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getFormattedErrors(), "You specified encryptionKeyUrl to use Server Side Encryption for Azure Managed disks with CMK, " +
                "but that feature is currently disabled. Get 'CDP_CB_AZURE_DISK_SSE_WITH_CMK' enabled for your account to use SSE with CMK.");
    }

    @Test
    public void testWhenUseMultipleResourceGroupsAndResourceEncryptionParameterKeyUrlAndEncryptionKeyResourceGroupAndEntitlementEnabledThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyUrl(KEY_URL)
                                .withEncryptionKeyResourceGroupName(RESOURCE_GROUP_NAME)
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(true);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenNoResourceGroupAndNoResourceEncryptionParameterAndEntitlementDisabledThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder().build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(false);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testNoResourceGroupAndNoResourceEncryptionParameterAndEntitlementEnabledThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder().build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(true);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenResourceEncryptionParameterDiskEncryptionSetIdThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder().build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withDiskEncryptionSetId("DummyDesId").build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getFormattedErrors(), "Specifying diskEncryptionSetId in request is Invalid. " +
                "Please specify encryptionKeyUrl to use Server Side Encryption for Azure Managed disks with CMK.");
    }

    @Test
    public void testCloudPlatform() {
        assertEquals(CloudPlatform.AZURE, underTest.getCloudPlatform());
    }

    @Test
    public void testWhenUseMultipleResourceGroupsAndResourceEncryptionParameterKeyUrlAndEncryptionKeyResourceGroupNameAndEntitlementEnabledThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyUrl(KEY_URL)
                                .withEncryptionKeyResourceGroupName(ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(true);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenUseMultipleResourceGroupsAndResourceEncryptionParameterKeyUrlAndEncryptionKeyResourceGroupNameAndNoEntitlementThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyUrl(KEY_URL)
                                .withEncryptionKeyResourceGroupName(ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(false);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getFormattedErrors(), "You specified encryptionKeyUrl to use Server Side Encryption for Azure Managed disks " +
                "with CMK, but that feature is currently disabled. Get 'CDP_CB_AZURE_DISK_SSE_WITH_CMK' enabled for your account to use SSE with CMK.");
    }

    @Test
    public void testWhenUseMultipleResourceGroupsAndEncryptionKeyResourceGroupNameAndNoEntitlementThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyResourceGroupName(ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(false);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getFormattedErrors(), "You specified encryptionKeyResourceGroupName to provide the resource group name which contains " +
                "the encryption keyfor Server Side Encryption of Azure Managed disks, but that feature is currently disabled. " +
                "Get 'CDP_CB_AZURE_DISK_SSE_WITH_CMK' enabled for your account to use SSE with CMK.");
    }

    @Test
    public void testWhenUseMultipleResourceGroupsAndEncryptionKeyResourceGroupNameAndNoResourceEncryptionParameterKeyUrlAndEntitlementThenError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE).build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyResourceGroupName(ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isAzureDiskSSEWithCMKEnabled(anyString())).thenReturn(true);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getFormattedErrors(), "You specified encryptionKeyResourceGroupName to provide the resource group " +
                "name which contains the encryption key for Server Side Encryption of Azure Managed disks. Please specify encryptionKeyUrl to use " +
                "Server Side Encryption for Azure Managed disks with CMK.");
    }

    private static class EnvironmentDtoBuilder {

        private static final String ACCOUNT_ID = "accountId";

        private final EnvironmentDto environmentDto = new EnvironmentDto();

        private final Builder parametersDtoBuilder = ParametersDto.builder();

        public EnvironmentDtoBuilder withAzureParameters(AzureParametersDto azureParametersDto) {
            parametersDtoBuilder.withAzureParameters(azureParametersDto);
            return this;
        }

        public EnvironmentDto build() {
            ParametersDto parametersDto = parametersDtoBuilder.build();
            environmentDto.setParameters(parametersDto);
            environmentDto.setAccountId(ACCOUNT_ID);
            return environmentDto;
        }
    }
}