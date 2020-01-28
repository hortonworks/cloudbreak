package com.sequenceiq.environment.environment.validation.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

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
    void testValidateDuringRequestWhenTheNetworkDoesNotContainAzureNetworkParams() {
        NetworkDto networkDto = testHelper.getNetworkDto(null, null, null, null, null, 1);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, null, validationResultBuilder);

        testHelper.checkErrorsPresent(validationResultBuilder, List.of(
                "The 'AZURE' related network parameters should be specified!",
                "Either the AZURE network id or cidr needs to be defined!"));
    }

    @Test
    void testValidateDuringRequestWhenTheAzureNetworkParamsDoesNotResourceGroupId() {
        AzureParams azureParams = testHelper.getAzureParams(true, true, false);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, null, validationResultBuilder);

        testHelper.checkErrorsPresent(validationResultBuilder, List.of(
                "If networkId is specified, then resourceGroupName must be specified too."));
    }

    @Test
    void testValidateDuringRequestWhenTheAzureNetworkParamsDoesNotContainNetworkId() {
        AzureParams azureParams = testHelper.getAzureParams(true, false, true);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, null, validationResultBuilder);

        testHelper.checkErrorsPresent(validationResultBuilder, List.of(
                "If resourceGroupName is specified, then networkId must be specified too.",
                "Either the AZURE network id or cidr needs to be defined!"));
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
    void testValidateDuringRequestWhenNetworkIdWithNoSubnets() {
        AzureParams azureParams = testHelper.getAzureParams(true, true, true);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, null);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, networkDto.getSubnetMetas(), resultBuilder);

        testHelper.checkErrorsPresent(resultBuilder, List.of(
                "If networkId (aNetworkId) and resourceGroupName (aResourceGroupId) are specified then subnet ids must be specified as well."));
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNetworkId() {
        AzureParams azureParams = testHelper.getAzureParams(true, true, true);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, networkDto.getSubnetMetas(), resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNoNetworkCidrAndNoNetworkId() {
        NetworkDto networkDto = testHelper.getNetworkDto(new AzureParams(), null, null, null, null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        testHelper.checkErrorsPresent(resultBuilder, List.of(
                "If AZURE subnet ids were provided then network id and resource group name have to be specified, too.",
                "Either the AZURE network id or cidr needs to be defined!")
        );
    }

    @Test
    void testValidateDuringRequestWhenNetworkCidrAndNoNetworkId() {
        AzureParams azureParams = testHelper.getAzureParams(true, false, false);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", null);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNetworkCidrAndNoAzureParams() {
        NetworkDto networkDto = testHelper.getNetworkDto(null, null, null, null, "0.0.0.0/0", null);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoAzureParams() {
        NetworkDto networkDto = testHelper.getNetworkDto(null, null, null, null, null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        testHelper.checkErrorsPresent(resultBuilder, List.of(
                "The 'AZURE' related network parameters should be specified!",
                "Either the AZURE network id or cidr needs to be defined!")
        );
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoNetworkId() {
        AzureParams azureParams = testHelper.getAzureParams(true, false, true);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        testHelper.checkErrorsPresent(resultBuilder, List.of(
                "If resourceGroupName is specified, then networkId must be specified too.",
                "Either the AZURE network id or cidr needs to be defined!")
        );
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoResourceGroupName() {
        AzureParams azureParams = testHelper.getAzureParams(true, true, false);
        NetworkDto networkDto = testHelper.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, null, resultBuilder);

        testHelper.checkErrorsPresent(resultBuilder, List.of(
                "If networkId is specified, then resourceGroupName must be specified too."));
    }

}