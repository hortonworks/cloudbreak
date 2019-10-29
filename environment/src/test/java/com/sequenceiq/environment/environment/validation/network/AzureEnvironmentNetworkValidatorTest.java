package com.sequenceiq.environment.environment.validation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AzureEnvironmentNetworkValidatorTest {

    private AzureEnvironmentNetworkValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureEnvironmentNetworkValidator();
    }

    @Test
    void testValidateWhenTheNetworkIsNull() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validate(null, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateWhenTheNetworkDoesNotContainAzureNetworkParams() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(null)
                .build();

        underTest.validate(networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'AZURE' related network parameters should be specified!", actual);
    }

    @Test
    void testValidateWhenTheAzureNetworkParamsDoesNotResourceGroupId() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = AzureParams.AzureParamsBuilder
                .anAzureParams()
                .withNetworkId("aNetworkId")
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .build();

        underTest.validate(networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'resource group's name(resourceGroupName)' parameter should be specified for the 'AZURE' environment specific network!", actual);
    }

    @Test
    void testValidateWhenTheAzureNetworkParamsDoesNotContainNetworkId() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = AzureParams.AzureParamsBuilder
                .anAzureParams()
                .withResourceGroupName("aResourceGroupId")
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .build();

        underTest.validate(networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'network identifier(networkId)' parameter should be specified for the 'AZURE' environment specific network!", actual);
    }

    @Test
    void testValidateWhenTheAzureNetworkParamsContainsRequiredFields() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AzureParams azureParams = AzureParams.AzureParamsBuilder
                .anAzureParams()
                .withNetworkId("aNetworkId")
                .withResourceGroupName("aResourceGroupId")
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .build();

        underTest.validate(networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }
}