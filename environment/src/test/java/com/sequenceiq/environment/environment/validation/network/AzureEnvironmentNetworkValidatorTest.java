package com.sequenceiq.environment.environment.validation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AzureEnvironmentNetworkValidatorTest {

    private final TestHelper testHelper = new TestHelper();

    private AzureEnvironmentNetworkValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureEnvironmentNetworkValidator();
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkIsNull() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringFlow(null, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkDoesNotContainAzureNetworkParams() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(null)
                .build();

        underTest.validateDuringRequest(networkDto, null, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'AZURE' related network parameters should be specified!", actual);
    }

    @Test
    void testValidateDuringFlowWhenTheAzureNetworkParamsDoesNotResourceGroupId() {
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

        underTest.validateDuringRequest(networkDto, null, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("If networkId is specified, then resourceGroupName must be specified too.", actual);
    }

    @Test
    void testValidateDuringFlowWhenTheAzureNetworkParamsDoesNotContainNetworkId() {
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

        underTest.validateDuringRequest(networkDto, null, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("If resourceGroupName is specified, then networkId must be specified too.", actual);
    }

    @Test
    void testValidateDuringFlowWhenTheAzureNetworkParamsContainsRequiredFields() {
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

        underTest.validateDuringFlow(networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringRequestWhenNetworkIdWithNoSubnetsOnAzure() {
        AzureParams azureParams = testHelper.getAzureParams(true, true, true);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, azureParams.getNetworkId(), null, null);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size(), validationResult.getFormattedErrors());
        String actual = validationResult.getErrors().get(0);
        assertEquals("If subnetId and resourceGroupName are specified then subnet ids must be specified as well.", actual);
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNetworkIdOnAzure() {
        AzureParams azureParams = testHelper.getAzureParams(true, true, true);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, azureParams.getNetworkId(), null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateDuringRequestWhenNetworkCidrAndNoNetworkIdOnAzure() {
        AzureParams azureParams = testHelper.getAzureParams(true, false, false);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, "0.0.0.0/0", null);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoAzureParamsOnAzure() {
        NetworkDto networkDto = testHelper.getNetworkDto(null, null, null, null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size(), validationResult.getFormattedErrors());
        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'AZURE' related network parameters should be specified!", actual);
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoNetworkIdOnAzure() {
        AzureParams azureParams = testHelper.getAzureParams(true, false, true);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, azureParams.getNetworkId(), null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size(), validationResult.getFormattedErrors());
        String actual = validationResult.getErrors().get(0);
        assertEquals("If resourceGroupName is specified, then networkId must be specified too.", actual);
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoResourceGroupNameOnAzure() {
        AzureParams azureParams = testHelper.getAzureParams(true, true, false);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, azureParams.getNetworkId(), null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size(), validationResult.getFormattedErrors());
        String actual = validationResult.getErrors().get(0);
        assertEquals("If networkId is specified, then resourceGroupName must be specified too.", actual);
    }

}