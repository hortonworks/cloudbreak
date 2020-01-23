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
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AwsEnvironmentNetworkValidatorTest {

    private TestHelper testHelper = new TestHelper();

    private AwsEnvironmentNetworkValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new AwsEnvironmentNetworkValidator();
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkIsNull() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringFlow(null, validationResultBuilder);

        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenTheNetworkDoesNotContainAwsNetworkParams() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
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
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AwsParams awsParams = AwsParams.AwsParamsBuilder
                .anAwsParams()
                .build();

        NetworkDto networkDto = NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAws(awsParams)
                .build();

        underTest.validateDuringFlow(networkDto, validationResultBuilder);

        ValidationResult validationResult = validationResultBuilder.build();
        assertTrue(validationResult.hasError());

        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'VPC identifier(vpcId)' parameter should be specified for the 'AWS' environment specific network!", actual);
    }

    @Test
    void testValidateDuringFlowWhenTheAwsNetworkParamsContainsVPCId() {
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        AwsParams awsParams = AwsParams.AwsParamsBuilder
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
    void testValidateDuringRequestWhenNetworkHasOneSubnetOnAws() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = testHelper.getNetworkDto(null, getAwsParams(), awsParams.getVpcId(), null, 1);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, testHelper.getSubnetMetas(1), resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size(), validationResult.getFormattedErrors());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.equals("There should be at least two subnets in the network")));
        assertTrue(actual.stream().anyMatch(item ->
                item.equals("The subnets in the vpc should be present at least in two different availability zones")));
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasTwoSubnetOnAws() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = testHelper.getNetworkDto(null, getAwsParams(), awsParams.getVpcId(), null, 2);

        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, testHelper.getSubnetMetas(2), resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError(), validationResult.getFormattedErrors());
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasTwoSubnetSubnetMetasHasThreeSubnetsOnAws() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = testHelper.getNetworkDto(null, getAwsParams(), awsParams.getVpcId(), null, 2);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, testHelper.getSubnetMetas(3), resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size(), validationResult.getFormattedErrors());
        String actual = validationResult.getErrors().get(0);
        assertEquals("Subnets of the environment () are not found in the vpc (vpcId). ", actual);
    }

    @Test
    void testValidateDuringRequestWhenNetworkHasTwoSubnetsWithSameAvailabilityZoneOnAws() {
        AwsParams awsParams = getAwsParams();
        NetworkDto networkDto = testHelper.getNetworkDto(null, getAwsParams(), awsParams.getVpcId(), null, 2);
        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            subnetMetas.put("key" + i, testHelper.getCloudSubnet("eu-west-1-a"));
        }
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, subnetMetas, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size(), validationResult.getFormattedErrors());
        String actual = validationResult.getErrors().get(0);
        assertEquals("The subnets in the vpc should be present at least in two different availability zones", actual);
    }

    private AwsParams getAwsParams() {
        return AwsParams.AwsParamsBuilder
                .anAwsParams()
                .withVpcId("vpcId")
                .build();
    }
}