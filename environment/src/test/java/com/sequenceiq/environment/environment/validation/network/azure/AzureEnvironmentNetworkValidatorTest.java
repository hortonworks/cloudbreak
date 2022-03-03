package com.sequenceiq.environment.environment.validation.network.azure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.environment.validation.network.NetworkTestUtils;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@ExtendWith(MockitoExtension.class)
class AzureEnvironmentNetworkValidatorTest {
    private static final String MY_SINGLE_RG = "mySingleRg";

    private AzureEnvironmentNetworkValidator underTest;

    @Mock
    private CloudNetworkService cloudNetworkService;

    @Mock
    private AzurePrivateEndpointValidator azurePrivateEndpointValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new AzureEnvironmentNetworkValidator(cloudNetworkService,
                azurePrivateEndpointValidator);
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringFlow(null, null, validationResultBuilder);

        assertTrue(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenTheAzureNetworkParamsContainsRequiredFields() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("", "");

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withSubnetMetas(Map.of())
                .build();

        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder().build())
                .build();

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenPrivateEndpointAndEnvCreationThenPrivateEndpointValidationsAreRun() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        verify(azurePrivateEndpointValidator).checkPrivateEndpointForExistingNetworkLink(validationResultBuilder, environmentDto, networkDto);
        verify(azurePrivateEndpointValidator).checkPrivateEndpointsWhenMultipleResourceGroup(validationResultBuilder, environmentDto,
                networkDto.getServiceEndpointCreation());
        verify(azurePrivateEndpointValidator).checkPrivateEndpointForExistingNetworkLink(validationResultBuilder, environmentDto, networkDto);
        verify(azurePrivateEndpointValidator).checkExistingPrivateDnsZoneWhenNotPrivateEndpoint(validationResultBuilder, networkDto);
    }

    @Test
    void testValidateDuringFlowWhenEnvironmentIsBeingEditedThenPrivateEndpointValidationsSkipped() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("", "networkResourceGroupName");

        NetworkDto networkDto = getNetworkDto(azureParams);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(getCloudSubnets(false));
        EnvironmentValidationDto environmentDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        environmentDto.setValidationType(ValidationType.ENVIRONMENT_EDIT);

        underTest.validateDuringFlow(environmentDto, networkDto, validationResultBuilder);

        verify(azurePrivateEndpointValidator, never()).checkPrivateEndpointForExistingNetworkLink(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkPrivateEndpointsWhenMultipleResourceGroup(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkPrivateEndpointForExistingNetworkLink(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkExistingPrivateDnsZoneWhenNotPrivateEndpoint(any(), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    private NetworkDto getNetworkDto(AzureParams azureParams) {
        return NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withSubnetMetas(Map.of())
                .withNetworkId("networkId")
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
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
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(environmentDto)
                .build();

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(Map.of(networkDto.getSubnetIds().stream().findFirst().get(),
                new CloudSubnet()));

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringFlow(environmentValidationDto, networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of("If networkId (aNetworkId) and resourceGroupName (aResourceGroupId) are specified then" +
                " subnet ids must be specified and should exist on azure as well. Given subnetids: [\"key1\", \"key0\"], existing ones: [\"key1\"]"));
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
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(AzureParams.builder().build(), null, null, null, null, 1);

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

    private Map<String, CloudSubnet> getCloudSubnets(boolean privateEndpointNetworkPoliciesEnabled) {
        CloudSubnet cloudSubnetOne = new CloudSubnet();
        cloudSubnetOne.putParameter("privateEndpointNetworkPolicies", privateEndpointNetworkPoliciesEnabled ? "enabled" : "disabled");
        cloudSubnetOne.setName("subnet-one");
        return Map.of("subnet-one", cloudSubnetOne);
    }

    private EnvironmentValidationDto environmentValidationDtoWithSingleRg(String name, ResourceGroupUsagePattern resourceGroupUsagePattern) {
        return EnvironmentValidationDto.builder()
                .withValidationType(ValidationType.ENVIRONMENT_CREATION)
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withParameters(ParametersDto.builder()
                                .withAzureParameters(
                                        AzureParametersDto.builder()
                                                .withResourceGroup(AzureResourceGroupDto.builder()
                                                        .withName(name)
                                                        .withResourceGroupUsagePattern(resourceGroupUsagePattern)
                                                        .build())
                                                .build()
                                ).build())
                        .build())
                .build();
    }

    private AzureParams getAzureParams(String networkId, String networkResourceGroupName) {
        return AzureParams.builder()
                .withNetworkId(networkId)
                .withResourceGroupName(networkResourceGroupName)
                .build();
    }
}
