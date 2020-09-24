package com.sequenceiq.environment.environment.validation.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.environment.validation.network.azure.AzureEnvironmentNetworkValidator;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AzureEnvironmentNetworkValidatorTest {

    private AzureEnvironmentNetworkValidator underTest;

    @Mock
    private CloudNetworkService cloudNetworkService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new AzureEnvironmentNetworkValidator(cloudNetworkService);
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringFlow(null, null, validationResultBuilder);

        assertTrue(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringRequestWhenTheNetworkDoesNotContainAzureNetworkParams() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, null, 1);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "The 'AZURE' related network parameters should be specified!",
                "Either the AZURE network id or cidr needs to be defined!"));
    }

    @Test
    void testValidateDuringRequestWhenTheAzureNetworkParamsDoesNotResourceGroupId() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, false);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "If networkId is specified, then resourceGroupName must be specified too."));
    }

    @Test
    void testValidateDuringRequestWhenTheAzureNetworkParamsDoesNotContainNetworkId() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, false, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "If resourceGroupName is specified, then networkId must be specified too.",
                "Either the AZURE network id or cidr needs to be defined!",
                "If subnetIds are specified, then networkId must be specified too."));
    }

    @Test
    void testValidateDuringFlowWhenTheAzureNetworkParamsContainsRequiredFields() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = AzureParams.builder()
                .withNetworkId("")
                .withResourceGroupName("")
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withSubnetMetas(Map.of())
                .build();

        EnvironmentDto environmentDto = new EnvironmentDto();

        underTest.validateDuringFlow(environmentDto, networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringRequestWhenNetworkIdWithNoSubnets() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, null);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "If networkId (aNetworkId) and resourceGroupName (aResourceGroupId) are specified then subnet ids must be specified as well."));
    }

    @Test
    void testValidateDuringRequestWhenNetworkIdWithSubnetsNotExistsOnAzure() {
        int numberOfSubnets = 2;
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, numberOfSubnets);
        EnvironmentDto environmentDto = new EnvironmentDto();

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(Map.of(networkDto.getSubnetIds().stream().findFirst().get(),
                new CloudSubnet()));

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringFlow(environmentDto, networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of("If networkId (aNetworkId) and resourceGroupName (aResourceGroupId) are specified then" +
                " subnet ids must be specified and should exist on azure as well. Given subnetids: [\"key1\", \"key0\"], exisiting ones: [\"key1\"]"));
    }

    @Test
    void testValidateDuringRequestWhenSubnetsWithNoNetworkId() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, false, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, null, 2);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of("Either the AZURE network id or cidr needs to be defined!",
                "If resourceGroupName is specified, then networkId must be specified too.",
                "If subnetIds are specified, then networkId must be specified too."));
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNetworkId() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNoNetworkCidrAndNoNetworkId() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(new AzureParams(), null, null, null, null, 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "If AZURE subnet ids were provided then network id and resource group name have to be specified, too.",
                "Either the AZURE network id or cidr needs to be defined!",
                "If subnetIds are specified, then networkId must be specified too.")
        );
    }

    @Test
    void testValidateDuringRequestWhenNetworkCidrAndNoNetworkId() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, false, false);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", null);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNetworkCidrAndNoAzureParams() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, "0.0.0.0/0", null);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoAzureParams() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, null, 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "The 'AZURE' related network parameters should be specified!",
                "Either the AZURE network id or cidr needs to be defined!")
        );
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoNetworkId() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, false, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "If resourceGroupName is specified, then networkId must be specified too.",
                "Either the AZURE network id or cidr needs to be defined!",
                "If subnetIds are specified, then networkId must be specified too.")
        );
    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoResourceGroupName() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, false);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "If networkId is specified, then resourceGroupName must be specified too."));
    }

}