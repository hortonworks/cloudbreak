package com.sequenceiq.environment.environment.validation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AwsParams.AwsParamsBuilder;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AwsEnvironmentNetworkValidatorTest {

    private AwsEnvironmentNetworkValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new AwsEnvironmentNetworkValidator();
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkIsNull() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringFlow(null, validationResultBuilder);

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

        underTest.validateDuringFlow(networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'AWS' related network parameters should be specified!", actual);
    }

    @Test
    void testValidateDuringFlowWhenTheAwsNetworkParamsDoesNotContainVPCId() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AwsParams awsParams = AwsParamsBuilder
                .anAwsParams()
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAws(awsParams)
                .build();

        underTest.validateDuringFlow(networkDto, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "The 'VPC identifier(vpcId)' parameter should be specified for the 'AWS' environment specific network!"));
    }

    @Test
    void testValidateDuringFlowWhenTheAwsNetworkParamsContainsVPCId() {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        AwsParams awsParams = AwsParamsBuilder
                .anAwsParams()
                .withVpcId("aVPCResourceIDFromAWS")
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAws(awsParams)
                .build();

        underTest.validateDuringFlow(networkDto, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasCidr() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, null, null, "1.2.3.4/16", 1);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, NetworkTestUtils.getSubnetMetas(1), validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasNoNetworkIdAndNoCidr() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, null, null, 1);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, NetworkTestUtils.getSubnetMetas(1), validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "There should be at least two subnets in the network",
                "The subnets in the vpc should be present at least in two different availability zones",
                "Either the AWS network id or cidr needs to be defined!"));
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasOneSubnet() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, awsParams.getVpcId(), null, 1);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, NetworkTestUtils.getSubnetMetas(1), validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "There should be at least two subnets in the network",
                "The subnets in the vpc should be present at least in two different availability zones")
        );
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasTwoSubnet() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, awsParams.getVpcId(), null, 2);

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, NetworkTestUtils.getSubnetMetas(2), validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasTwoSubnetSubnetMetasHasThreeSubnets() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, awsParams.getVpcId(), null, 2);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, NetworkTestUtils.getSubnetMetas(3), validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "Subnets of the environment () are not found in the vpc (vpcId)."
        ));
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasTwoSubnetsWithSameAvailabilityZone() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, getAwsParams(), null, awsParams.getVpcId(), null, 2);
        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            subnetMetas.put("key" + i, NetworkTestUtils.getCloudSubnet("eu-west-1-a"));
        }
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, subnetMetas, validationResultBuilder);

        NetworkTestUtils.checkErrorsPresent(validationResultBuilder, List.of(
                "The subnets in the vpc should be present at least in two different availability zones"
        ));
    }

    private AwsParams getAwsParams() {
        return AwsParamsBuilder
                .anAwsParams()
                .withVpcId("vpcId")
                .build();
    }
}