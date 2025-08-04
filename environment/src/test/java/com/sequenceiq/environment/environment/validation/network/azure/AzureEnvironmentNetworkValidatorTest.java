package com.sequenceiq.environment.environment.validation.network.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudSubnetParametersService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.environment.domain.Region;
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

    @Mock
    private AzureCloudSubnetParametersService azureCloudSubnetParametersService;

    @BeforeEach
    void setUp() {
        underTest = new AzureEnvironmentNetworkValidator(cloudNetworkService, azurePrivateEndpointValidator, azureCloudSubnetParametersService);
        ReflectionTestUtils.setField(underTest, "azureAvailabilityZones", Set.of("1", "2", "3"));
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
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withSubnetMetas(Map.of())
                .build();

        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(getEnvironmentDtoWithRegion())
                .build();

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenPrivateEndpointAndEnvCreationThenPrivateEndpointValidationsAreRun() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        verify(azurePrivateEndpointValidator).checkNewPrivateDnsZone(validationResultBuilder, environmentDto, networkDto);
        verify(azurePrivateEndpointValidator).checkMultipleResourceGroup(validationResultBuilder, environmentDto,
                networkDto);
        verify(azurePrivateEndpointValidator).checkNewPrivateDnsZone(validationResultBuilder, environmentDto, networkDto);
        verify(azurePrivateEndpointValidator).checkExistingManagedPrivateDnsZone(validationResultBuilder, environmentDto, networkDto);
    }

    @Test
    void testValidateDuringFlowWhenFlexiblePrivateEndpointAndEnvEditThenPrivateEndpointValidationsAreRun() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());
        NetworkDto networkDto = NetworkTestUtils
                .getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .withAzure(AzureParams.builder().withDatabasePrivateDnsZoneId("privateDnsZone").build())
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ValidationType.ENVIRONMENT_EDIT,
                ResourceGroupUsagePattern.USE_SINGLE);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        verify(azurePrivateEndpointValidator).checkNewPrivateDnsZone(validationResultBuilder, environmentDto, networkDto);
        verify(azurePrivateEndpointValidator).checkMultipleResourceGroup(validationResultBuilder, environmentDto,
                networkDto);
        verify(azurePrivateEndpointValidator).checkNewPrivateDnsZone(validationResultBuilder, environmentDto, networkDto);
        verify(azurePrivateEndpointValidator).checkExistingManagedPrivateDnsZone(validationResultBuilder, environmentDto, networkDto);
    }

    @Test
    void testValidateDuringFlowWhenEnvEditAndDnsZoneNotChangedThenPrivateEndpointValidationsSkipped() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());

        NetworkDto networkDto = getNetworkDto(azureParams);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(getCloudSubnets(false));
        EnvironmentValidationDto environmentDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        environmentDto.setValidationType(ValidationType.ENVIRONMENT_EDIT);

        underTest.validateDuringFlow(environmentDto, networkDto, validationResultBuilder);

        verify(azurePrivateEndpointValidator, never()).checkNewPrivateDnsZone(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkMultipleResourceGroup(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkNewPrivateDnsZone(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkExistingManagedPrivateDnsZone(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkExistingRegisteredOnlyPrivateDnsZone(any(), any(), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenEnvironmentIsBeingEditedThenPrivateEndpointValidationsSkipped() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());

        NetworkDto networkDto = getNetworkDto(azureParams);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(getCloudSubnets(false));
        EnvironmentValidationDto environmentDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        environmentDto.setValidationType(ValidationType.ENVIRONMENT_EDIT);

        underTest.validateDuringFlow(environmentDto, networkDto, validationResultBuilder);

        verify(azurePrivateEndpointValidator, never()).checkNewPrivateDnsZone(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkMultipleResourceGroup(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkNewPrivateDnsZone(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkExistingManagedPrivateDnsZone(any(), any(), any());
        verify(azurePrivateEndpointValidator, never()).checkExistingRegisteredOnlyPrivateDnsZone(any(), any(), any());
        assertFalse(validationResultBuilder.build().hasError());
    }

    private static Stream<Set<String>> provideFlexibleServerSubnetArguments() {
        return Stream.of(
                Set.of("azure/flexibleSubnet"),
                Set.of("flexibleSubnet1"),
                Set.of("azure/flexibleSubnet, flexibleSubnet1"),
                Set.of("azure/flexibleSubnet1, azure/flexibleSubnet2"),
                Set.of("flexibleSubnet1, flexibleSubnet2")
        );
    }

    @ParameterizedTest
    @MethodSource("provideFlexibleServerSubnetArguments")
    void testValidateDuringFlowWhenFlexibleServerSubnetIdsAreValid(Set<String> input) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(input);
        NetworkDto networkDto = NetworkTestUtils
                .getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        Set<String> subnetNames = input.stream()
                .map(subnet -> {
                    if (subnet.contains("/")) {
                        return StringUtils.substringAfterLast(subnet, "/");
                    } else {
                        return subnet;
                    }
                }).collect(Collectors.toSet());
        Map<String, CloudSubnet> subnetToCloudSubnet = subnetNames.stream().collect(Collectors.toMap(Function.identity(), sn ->
                new CloudSubnet.Builder()
                    .id(sn)
                    .build()
        ));
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(subnetToCloudSubnet);
        when(cloudNetworkService.getSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto, subnetNames))
                .thenReturn(subnetToCloudSubnet);
        when(azureCloudSubnetParametersService.isFlexibleServerDelegatedSubnet(any(CloudSubnet.class))).thenReturn(true);
        when(cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto()))
                .thenReturn(Set.of(azureParams.getNetworkId()));

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenFlexibleServerSubnetsNotChanged() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        NetworkDto networkDto = NetworkTestUtils
                .getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ValidationType.ENVIRONMENT_EDIT,
                ResourceGroupUsagePattern.USE_SINGLE);
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.ofEntries(Map.entry("flexibleSubnet", new CloudSubnet())));
        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @ParameterizedTest
    @EnumSource(ServiceEndpointCreation.class)
    void testValidateDuringFlowWhenFlexibleServerSubnetsDeleted(ServiceEndpointCreation serviceEndpointCreation) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        NetworkDto networkDto = NetworkTestUtils
                .getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .withServiceEndpointCreation(serviceEndpointCreation)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ValidationType.ENVIRONMENT_EDIT,
                ResourceGroupUsagePattern.USE_SINGLE);
        environmentValidationDto.getEnvironmentDto().getNetwork().getAzure().setFlexibleServerSubnetIds(Set.of("azure/flexibleSubnet"));
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.ofEntries(Map.entry("flexibleSubnet", new CloudSubnet())));
        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        if (serviceEndpointCreation == ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT) {
            assertFalse(validationResult.hasError());
        } else {
            assertTrue(validationResult.hasError());
            assertTrue(validationResult.getErrors().stream()
                    .anyMatch(error -> error.startsWith("Deletion of all Flexible server delegated subnets is not supported")));
        }
    }

    @Test
    void testValidateDuringFlowWhenFlexibleServerSubnetsMissingOnProvider() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(Set.of("azure/flexibleSubnet"));
        NetworkDto networkDto = NetworkTestUtils
                .getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.ofEntries(Map.entry("flexibleSubnet", new CloudSubnet())));
        when(cloudNetworkService.getSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto, Set.of("flexibleSubnet")))
                .thenReturn(Map.of());
        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertTrue(validationResult.getErrors().stream()
                .anyMatch(error -> error.startsWith("The following flexible server delegated subnets are not found on the provider side")));
    }

    @Test
    void testValidateDuringFlowWhenFlexibleServerSubnetIdsAndPrivateEndpointsAreSpecified() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(Set.of("azure/flexibleSubnet"));

        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.ofEntries(Map.entry("flexibleSubnet", new CloudSubnet())));
        when(cloudNetworkService.getSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto, Set.of("flexibleSubnet")))
                .thenReturn(Map.ofEntries(Map.entry(networkDto.getSubnetIds().iterator().next(), new CloudSubnet())));
        when(cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto()))
                .thenReturn(Set.of(azureParams.getNetworkId()));
        when(azureCloudSubnetParametersService.isFlexibleServerDelegatedSubnet(any(CloudSubnet.class))).thenReturn(true);
        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResultBuilder.build().hasError());
        assertEquals(1, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().stream()
                .anyMatch(error -> error.startsWith("Both Private Endpoint and Flexible Server delegated subnet(s) are specified in the request. " +
                        "As they are mutually exclusive, please specify only one of them and retry.")));
    }

    @Test
    void testValidateDuringFlowWhenFlexibleServerSubnetIdsAreInvalid() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        azureParams.setFlexibleServerSubnetIds(Set.of("azure/flexibleSubnet"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.ofEntries(Map.entry("flexibleSubnet", new CloudSubnet())));
        when(cloudNetworkService.getSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto, Set.of("flexibleSubnet")))
                .thenReturn(Map.ofEntries(Map.entry(networkDto.getSubnetIds().iterator().next(), new CloudSubnet())));
        when(azureCloudSubnetParametersService.isFlexibleServerDelegatedSubnet(any(CloudSubnet.class))).thenReturn(false);
        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResultBuilder.build().hasError());
        assertTrue(validationResult.getErrors().stream()
                .anyMatch(error -> error.startsWith("The following subnets are not delegated to flexible servers")));
    }

    @Test
    void testValidateDuringFlowWhenFlexibleServerSubnetIdsAreInvalidAndChecked() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "networkResourceGroupName");
        Set<String> flexibleSubnets = Set.of("flexibleSubnet");
        azureParams.setFlexibleServerSubnetIds(flexibleSubnets);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0, RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.ofEntries(Map.entry("flexibleSubnet", new CloudSubnet())));
        when(cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto()))
                .thenReturn(Set.of(azureParams.getNetworkId()));
        Map<String, CloudSubnet> subnetToCloudSubnet = flexibleSubnets.stream().collect(Collectors.toMap(Function.identity(), sn ->
                new CloudSubnet.Builder()
                    .id(sn)
                    .build()
        ));
        when(cloudNetworkService.getSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto, flexibleSubnets))
                .thenReturn(subnetToCloudSubnet);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult result = validationResultBuilder.build();
        assertTrue(result.hasError());
        assertEquals("The following subnets are not delegated to flexible servers: flexibleSubnet", result.getFormattedErrors());
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
                "Azure existing networkId needs to be defined, environment creation with new network is not supported."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sOMEcRAZYNEtWORkIdnaME", "SOMECRAZYNETWORKIDNAME", "somecrazynetworkidname"})
    void testExistingWithCaseInsensitivity(String inputNetworkName) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams(inputNetworkName, MY_SINGLE_RG);
        String defaultSubnetName = "default";
        String additionalName = "subnet1";

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withAzure(azureParams)
                .withNetworkId(inputNetworkName)
                .withResourceCrn(MY_SINGLE_RG)
                .withRegistrationType(RegistrationType.EXISTING)
                .withSubnetMetas(Map.of(defaultSubnetName, new CloudSubnet(), additionalName, new CloudSubnet()))
                .build();

        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withValidationType(ValidationType.ENVIRONMENT_CREATION)
                .withEnvironmentDto(getEnvironmentDtoWithRegion())
                .build();
        environmentValidationDto.getEnvironmentDto().setNetwork(networkDto);

        when(cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto()))
                .thenReturn(Set.of("someOtherNetwork", inputNetworkName.toLowerCase()));
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto)).thenReturn(networkDto.getSubnetMetas());

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
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
                "Azure existing networkId needs to be defined, environment creation with new network is not supported.",
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
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, numberOfSubnets);
        EnvironmentDto environmentDto = getEnvironmentDtoWithRegion();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(environmentDto)
                .build();

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(Map.of(networkDto.getSubnetIds().stream().findFirst().get(),
                new CloudSubnet()));

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringFlow(environmentValidationDto, networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of("If networkId (aNetworkId) and resourceGroupName (aResourceGroupId) are specified then" +
                " subnet IDs must be specified and should exist on Azure as well. Given subnet IDs: [\"key1\", \"key0\"], existing ones: [\"key1\"], " +
                "in region: [East US]"));
    }

    @Test
    void testValidateDuringRequestWhenSubnetsWithNoNetworkId() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, false, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, null, 2);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "Azure existing networkId needs to be defined, environment creation with new network is not supported.",
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
                "If AZURE subnet IDs were provided then network id and resource group name have to be specified, too.",
                "Azure existing networkId needs to be defined, environment creation with new network is not supported.",
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
        assertTrue(validationResult.hasError());
        assertEquals("Azure existing networkId needs to be defined, environment creation with new network is not supported.",
                validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNetworkCidrAndNoAzureParams() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, "0.0.0.0/0", null);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals("Azure existing networkId needs to be defined, environment creation with new network is not supported.",
                validationResult.getFormattedErrors());    }

    @Test
    void testValidateDuringRequestWhenNoNetworkCidrAndNoAzureParams() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, null, 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "The 'AZURE' related network parameters should be specified!",
                "Azure existing networkId needs to be defined, environment creation with new network is not supported.")
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
                "Azure existing networkId needs to be defined, environment creation with new network is not supported.",
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

    @Test
    void testValidateDuringRequestWhenOnlyOneAvailabilityZoneIsGiven() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();

        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenOneAvailabilityZonesIsInvalid() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "Invalid1"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "Availability zones Invalid1 are not valid. Valid availability zones are 1,2,3."));
    }

    @Test
    void testValidateDuringRequestWhenMultipleAvailabilityZonesAreInvalid() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "Invalid1", "Invalid2"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "Availability zones Invalid1,Invalid2 are not valid. Valid availability zones are 1,2,3."));
    }

    @Test
    void testValidateDuringRequestWhenExistingZonesAreEmpty() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "2", "3"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(null, networkDto, resultBuilder);

        assertFalse(resultBuilder.build().hasError());

    }

    @Test
    void testValidateDuringRequestWhenExistingZonesAreContained() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "2", "3"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(setUpExistingEnvironment(Set.of("1", "2")), networkDto, resultBuilder);
        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringRequestWhenExistingZonesAreNotContained() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("2", "3"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateDuringRequest(setUpExistingEnvironment(Set.of("1", "3")), networkDto, resultBuilder);
        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "Provided Availability Zones for environment do not contain the existing Availability Zones. " +
                        "Provided Availability Zones : 2,3. Existing Availability Zones : 1,3"));
    }

    @Test
    void testValidateDuringFlowWhenEndpointGatewaySubnetIdNotInVnet() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("vnet", "resourceGroup");
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());

        Map<String, CloudSubnet> eagMetas = Map.of("eagsubnet1", new CloudSubnet.Builder()
                .id("eid1")
                .name("eagsubnet1")
                .build()
        );
        Map<String, CloudSubnet> metas = Map.of("subnet1", new CloudSubnet.Builder()
                .id("id1")
                .name("subnet1")
                .build()
        );
        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withSubnetMetas(metas)
                .withEndpointGatewaySubnetMetas(eagMetas)
                .build();

        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(getEnvironmentDtoWithRegion())
                .build();

        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class))).thenReturn(metas);

        Map<String, CloudSubnet> providerSubnets = Map.of("providerSubnet1",
                new CloudSubnet.Builder()
                .id("pnid1")
                .name("providerSubnet1")
                .build(),
                "providerSubnet2",
                new CloudSubnet.Builder()
                .id("pnid2")
                .name("providerSubnet2")
                .build());
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class))).thenReturn(providerSubnets);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).hasSize(1);
        assertThat(validationResult.getErrors().get(0)).startsWith(
                "If networkId (vnet) and resourceGroupName (resourceGroup) are specified then "
                        + "endpoint gateway subnet IDs must be specified and should exist on Azure as well.");
    }

    @Test
    void testValidateDuringFlowWhenEndpointGatewaySubnetIdIsInVnet() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("vnet", "resourceGroup");
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());

        Map<String, CloudSubnet> eagMetas = Map.of("eagsubnet1",
                new CloudSubnet.Builder()
                        .id("eid1")
                        .name("eagsubnet1")
                        .build());
        Map<String, CloudSubnet> metas = Map.of("subnet1",
                new CloudSubnet.Builder()
                        .id("id1")
                        .name("subnet1")
                        .build()
        );
        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withSubnetMetas(metas)
                .withEndpointGatewaySubnetMetas(eagMetas)
                .build();

        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(getEnvironmentDtoWithRegion())
                .build();

        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class))).thenReturn(metas);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class))).thenReturn(eagMetas);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isFalse();
    }

    private Map<String, CloudSubnet> getCloudSubnets(boolean privateEndpointNetworkPoliciesEnabled) {
        CloudSubnet cloudSubnetOne = new CloudSubnet();
        cloudSubnetOne.putParameter("privateEndpointNetworkPolicies", privateEndpointNetworkPoliciesEnabled ? "enabled" : "disabled");
        cloudSubnetOne.setName("subnet-one");
        return Map.of("subnet-one", cloudSubnetOne);
    }

    private EnvironmentValidationDto environmentValidationDtoWithSingleRg(String name, ResourceGroupUsagePattern resourceGroupUsagePattern) {
        return environmentValidationDtoWithSingleRg(name, ValidationType.ENVIRONMENT_CREATION, resourceGroupUsagePattern);
    }

    private EnvironmentValidationDto environmentValidationDtoWithSingleRg(String name, ValidationType validationType,
            ResourceGroupUsagePattern resourceGroupUsagePattern) {
        Region usWestRegion = new Region();
        usWestRegion.setName("eastus");
        usWestRegion.setDisplayName("East US");
        return EnvironmentValidationDto.builder()
                .withValidationType(validationType)
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withAccountId("acc")
                        .withParameters(ParametersDto.builder()
                                .withAzureParametersDto(
                                        AzureParametersDto.builder()
                                                .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
                                                        .withName(name)
                                                        .withResourceGroupUsagePattern(resourceGroupUsagePattern)
                                                        .build())
                                                .build()
                                ).build())
                        .withRegions(Set.of(usWestRegion))
                        .withNetwork(NetworkDto.builder().withAzure(AzureParams.builder().build()).build())
                        .build())
                .build();
    }

    private AzureParams getAzureParams(String networkId, String networkResourceGroupName) {
        return AzureParams.builder()
                .withNetworkId(networkId)
                .withResourceGroupName(networkResourceGroupName)
                .build();
    }

    private static EnvironmentDto getEnvironmentDtoWithRegion() {
        Region usWestRegion = new Region();
        usWestRegion.setName("eastus");
        usWestRegion.setDisplayName("East US");
        return EnvironmentDto.builder()
                .withRegions(Set.of(usWestRegion))
                .withNetwork(NetworkDto.builder().withAzure(AzureParams.builder().build()).build())
                .build();
    }

    private EnvironmentValidationDto setUpExistingEnvironment(Set<String> availabilityZones) {
        AzureParams azureParams = mock(AzureParams.class);
        when(azureParams.getAvailabilityZones()).thenReturn(availabilityZones);
        NetworkDto networkDto = mock(NetworkDto.class);
        when(networkDto.getAzure()).thenReturn(azureParams);
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentDto.getNetwork()).thenReturn(networkDto);
        EnvironmentValidationDto environmentValidationDto = mock(EnvironmentValidationDto.class);
        when(environmentValidationDto.getEnvironmentDto()).thenReturn(environmentDto);
        return environmentValidationDto;
    }

    @Test
    void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AZURE, underTest.getCloudPlatform());
    }

    @Test
    void testValidateNetworkIdWhenNetworkNotFound() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("nonExistentNetwork", "resourceGroup");
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0,
                        RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        when(cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto()))
                .thenReturn(Set.of("someOtherNetwork"));
        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.of("subnet1", new CloudSubnet()));

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertTrue(validationResult.getErrors().stream()
                .anyMatch(error -> error.contains("Unable to find network on Azure with the following name: nonExistentNetwork")));
    }

    @Test
    void testValidateDuringFlowWhenEnvironmentValidationDtoIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        NetworkDto networkDto = getNetworkDto(getAzureParams("networkId", "resourceGroup"));

        underTest.validateDuringFlow(null, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals("Internal validation error", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringFlowWhenEnvironmentDtoIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        NetworkDto networkDto = getNetworkDto(getAzureParams("networkId", "resourceGroup"));
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().build();

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals("Internal validation error", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringFlowWhenNetworkDtoIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        underTest.validateDuringFlow(environmentValidationDto, null, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals("Internal validation error", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNetworkDtoIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(null, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateDuringRequestWithEnvironmentValidationDtoWhenNetworkDtoIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        underTest.validateDuringRequest(environmentValidationDto, null, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateAvailabilityZonesWhenEnvironmentValidationDtoIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "2"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);

        underTest.validateDuringRequest(null, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateAvailabilityZonesWhenEnvironmentDtoIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "2"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().build();

        underTest.validateDuringRequest(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateAvailabilityZonesWhenNetworkIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "2"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder().build())
                .build();

        underTest.validateDuringRequest(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateAvailabilityZonesWhenAzureParamsIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        azureParams.setAvailabilityZones(Set.of("1", "2"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withNetwork(NetworkDto.builder().build())
                        .build())
                .build();

        underTest.validateDuringRequest(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testCheckPrivateDnsZoneIdWhenExistingDnsZoneDeletion() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "resourceGroup");
        azureParams.setFlexibleServerSubnetIds(new HashSet<>());
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0,
                        RegistrationType.EXISTING)
                // No DNS zone in new network
                .withAzure(AzureParams.builder().build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withValidationType(ValidationType.ENVIRONMENT_EDIT)
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withRegions(Set.of(new Region()))
                        .withNetwork(NetworkDto.builder()
                                .withAzure(AzureParams.builder()
                                        .withDatabasePrivateDnsZoneId("existingDnsZone")
                                        .build())
                                .build())
                        .build())
                .build();

        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.of("subnet1", new CloudSubnet()));

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        verify(azurePrivateEndpointValidator).checkExistingDnsZoneDeletion(ValidationType.ENVIRONMENT_EDIT, "existingDnsZone", null, validationResultBuilder);
    }

    @Test
    void testConvertFlexibleServerSubnetIdWithSlash() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "resourceGroup");
        azureParams.setFlexibleServerSubnetIds(Set.of("azure/resource/group/subnet"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0,
                        RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.of("subnet", new CloudSubnet()));
        when(cloudNetworkService.getSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto, Set.of("subnet")))
                .thenReturn(Map.of("subnet", new CloudSubnet()));
        when(azureCloudSubnetParametersService.isFlexibleServerDelegatedSubnet(any(CloudSubnet.class))).thenReturn(true);
        when(cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto()))
                .thenReturn(Set.of(azureParams.getNetworkId()));

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testConvertFlexibleServerSubnetIdWithoutSlash() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "resourceGroup");
        azureParams.setFlexibleServerSubnetIds(Set.of("subnet"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0,
                        RegistrationType.EXISTING)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.of("subnet", new CloudSubnet()));
        when(cloudNetworkService.getSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto, Set.of("subnet")))
                .thenReturn(Map.of("subnet", new CloudSubnet()));
        when(azureCloudSubnetParametersService.isFlexibleServerDelegatedSubnet(any(CloudSubnet.class))).thenReturn(true);
        when(cloudNetworkService.retrieveCloudNetworks(environmentValidationDto.getEnvironmentDto()))
                .thenReturn(Set.of(azureParams.getNetworkId()));

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateDuringFlowWhenPrivateEndpointAndFlexibleServerSubnetsSpecified() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AzureParams azureParams = getAzureParams("networkId", "resourceGroup");
        azureParams.setFlexibleServerSubnetIds(Set.of("flexibleSubnet"));
        NetworkDto networkDto = NetworkTestUtils.getNetworkDtoBuilder(azureParams, null, null, azureParams.getNetworkId(), null, 1, 0,
                        RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        EnvironmentValidationDto environmentValidationDto = environmentValidationDtoWithSingleRg(MY_SINGLE_RG, ResourceGroupUsagePattern.USE_SINGLE);

        when(cloudNetworkService.retrieveSubnetMetadata(environmentValidationDto.getEnvironmentDto(), networkDto))
                .thenReturn(Map.of("subnet1", new CloudSubnet()));

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertTrue(validationResult.getErrors().stream()
                .anyMatch(error -> error.contains("Both Private Endpoint and Flexible Server delegated subnet(s) are specified")));
    }
}