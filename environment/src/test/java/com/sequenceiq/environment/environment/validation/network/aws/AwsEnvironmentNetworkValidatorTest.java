package com.sequenceiq.environment.environment.validation.network.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.network.NetworkTestUtils;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AwsEnvironmentNetworkValidatorTest {

    private static final String ENV_NAME = "someenv";

    private static final String NETWORK_CIDR = "0.0.0.0/16";

    private AwsEnvironmentNetworkValidator underTest;

    @Mock
    private CloudNetworkService cloudNetworkService;

    @BeforeEach
    void setUp() {
        underTest = new AwsEnvironmentNetworkValidator(cloudNetworkService);
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringFlow(mock(EnvironmentValidationDto.class), null, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkDoesNotContainAwsNetworkParams() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAws(null)
                .build();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'AWS' related network parameters should be specified!", actual);
    }

    @Test
    void testValidateDuringFlowWhenTheAwsNetworkParamsDoesNotContainVPCId() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AwsParams awsParams = AwsParams
                .builder()
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAws(awsParams)
                .build();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "The 'VPC identifier(vpcId)' parameter should be specified for the 'AWS' environment specific network!"));
    }

    @Test
    void testValidateDuringFlowWhenTheAwsNetworkParamsContainsVPCId() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AwsParams awsParams = AwsParams
                .builder()
                .withVpcId("aVPCResourceIDFromAWS")
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAws(awsParams)
                .build();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasCidr() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, "1.2.3.4/16", 1, RegistrationType.CREATE_NEW);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenNetworkHasNoNetworkIdAndNoCidr() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, null, null, 1, RegistrationType.EXISTING);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of("Either the AWS network ID or CIDR needs to be defined!"));
    }

    @Test
    void testValidateDuringFlowWhenNetworkHasOneSubnet() {
        int amountOfSubnets = 1;
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, awsParams.getVpcId(), null,
                amountOfSubnets, RegistrationType.EXISTING);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        for (int i = 0; i < amountOfSubnets; i++) {
            subnetMetas.put("key" + i, NetworkTestUtils.getCloudSubnet("eu-west-1-a"));
        }

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(subnetMetas);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder,
                List.of("There should be at least two Subnets in the environment network configuration.")
        );
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasTwoSubnet() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, awsParams.getVpcId(), null, 2);

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringFlowWhenNetworkHasTwoSubnetSubnetMetasHasThreeSubnets() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, awsParams.getVpcId(), null, 2, RegistrationType.EXISTING);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENV_NAME);
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(new LinkedHashMap<>());

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "Subnet IDs of the environment (someenv) are not found in the VPC (key1, key0). All subnets are expected to belong to the same VPC."
        ));
    }

    @Test
    void testValidateDuringFlowWhenNetworkHasTwoSubnetsWithSameAvailabilityZone() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, awsParams, null, awsParams.getVpcId(), null, 2, RegistrationType.EXISTING);
        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            subnetMetas.put("key" + i, NetworkTestUtils.getCloudSubnet("eu-west-1-a", "name"));
        }
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(subnetMetas);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "The Subnets in the VPC (eu-west-1-a) should be present at least in two different availability zones, " +
                        "but they are present only in availability zone name, name. Please add subnets to the environment " +
                        "from the required number of different availability zones."
        ));
    }

    @Test
    void testValidateDuringFlowWhenNetworkDtoIsNullThenNoCloudServiceRelatedCallHappens() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringFlow(new EnvironmentValidationDto(), null, validationResultBuilder);

        verify(cloudNetworkService, times(0)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verify(cloudNetworkService, times(0)).retrieveSubnetMetadata(any(Environment.class), any(NetworkDto.class));

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateDuringFlowWhenNetworkTypeIsNewThenNoValidationHappensEvenIfTheActualNetworkDtoIsInvalid() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, null, 999, RegistrationType.CREATE_NEW);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringFlow(new EnvironmentValidationDto(), networkDto, validationResultBuilder);

        verify(cloudNetworkService, times(0)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verify(cloudNetworkService, times(0)).retrieveSubnetMetadata(any(Environment.class), any(NetworkDto.class));

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testCheckNullableWhenNetworkIsNull() {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.checkNullable(CloudPlatform.AWS, null, resultBuilder);
        ValidationResult actual = resultBuilder.build();
        assertThat(actual.hasError()).isTrue();
        assertThat(actual.getFormattedErrors()).isEqualTo("Environment network cannot be null");
    }

    @Test
    public void testCheckNullableWhenNetworkIsNotNull() {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        NetworkDto networkDto = mock(NetworkDto.class);
        underTest.checkNullable(CloudPlatform.AWS, networkDto, resultBuilder);
        ValidationResult actual = resultBuilder.build();
        assertThat(actual.hasError()).isFalse();
    }

    @Test
    void testValidateDuringFlowWhenEndpointGatewaySubnetIdNotInVPC() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, "1.2.3.4/16", 2, 2, RegistrationType.EXISTING);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENV_NAME);
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        subnetMetas.put("key0", NetworkTestUtils.getCloudSubnet("eu-west-1-a"));
        subnetMetas.put("key1", NetworkTestUtils.getCloudSubnet("eu-west-1-b"));

        Map<String, CloudSubnet> endpointGwSubnetMetas = new HashMap<>();
        endpointGwSubnetMetas.put("key0", NetworkTestUtils.getCloudSubnet("eu-west-1-a"));

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(subnetMetas);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto)).thenReturn(endpointGwSubnetMetas);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertThat(validationResult.getFormattedErrors())
                .startsWith("Endpoint gateway subnet IDs of the environment (someenv) are not found in the VPC (key1).");
    }

    @Test
    void testValidateDuringFlowWhenEndpointGatewaySubnetIdInVPC() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, "1.2.3.4/16", 4, 4, RegistrationType.EXISTING);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENV_NAME);
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        subnetMetas.put("subnet0", NetworkTestUtils.getCloudSubnet("eu-west-1-a", "subnet0"));
        subnetMetas.put("subnet1", NetworkTestUtils.getCloudSubnet("eu-west-1-b", "subnet1"));
        subnetMetas.put("subnet2", NetworkTestUtils.getCloudSubnet("eu-west-1-b",  "subnet2"));
        subnetMetas.put("subnet4", NetworkTestUtils.getCloudSubnet("eu-west-1-a", "subnet4"));

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(subnetMetas);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto)).thenReturn(subnetMetas);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());
        assertThat(validationResult.getFormattedErrors())
                .startsWith("Environment 'someenv' has been requested with invalid public endpoint access gateway setup. The selected subnets must have " +
                        "different Availability Zones, which means select one subnet per zone only. But")
                .contains("are from zone 'eu-west-1-b'")
                .contains("are from zone 'eu-west-1-a'");
    }

    @Test
    void testValidateDuringFlowWhenEndpointGatewaySubnetIdInVPCAndOneSubnetPerAZ() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, "1.2.3.4/16", 2, 2, RegistrationType.EXISTING);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENV_NAME);
        environmentDto.setNetwork(networkDto);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        subnetMetas.put("key0", NetworkTestUtils.getCloudSubnet("eu-west-1-a"));
        subnetMetas.put("key1", NetworkTestUtils.getCloudSubnet("eu-west-1-b"));

        Map<String, CloudSubnet> endpointGwSubnetMetas = new HashMap<>();
        endpointGwSubnetMetas.put("key0", NetworkTestUtils.getCloudSubnet("eu-west-1-a"));
        endpointGwSubnetMetas.put("key1", NetworkTestUtils.getCloudSubnet("eu-west-1-b"));

        when(cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto)).thenReturn(subnetMetas);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto)).thenReturn(endpointGwSubnetMetas);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    private AwsParams getAwsParams() {
        return AwsParams
                .builder()
                .withVpcId("vpcId")
                .build();
    }

}
