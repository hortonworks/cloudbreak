package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentTestData;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

public class NetworkCreationValidatorTest {

    @Mock
    private NetworkService networkService;

    @InjectMocks
    private NetworkCreationValidator underTest;

    private Environment environment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        environment = EnvironmentTestData.newTestEnvironment();
    }

    @Test
    void testValidateWhenNetworkCidrAndNetworkIdOnAzure() {
        AzureParams azureParams = getAzureParams(true);
        NetworkDto networkDto = getNetworkDto(azureParams, null, "aNetworkId", "0.0.0.0/0", 1);
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);
        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals("The 'AZURE' network id must not be defined if cidr is defined!", actual);
    }

    @Test
    void testValidateWhenNoNetworkCidrAndNetworkIdOnAzure() {
        AzureParams azureParams = getAzureParams(true);
        NetworkDto networkDto = getNetworkDto(azureParams, null, "aNetworkId", null, 1);

        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNetworkCidrAndNoNetworkIdOnAzure() {
        AzureParams azureParams = getAzureParams(true);
        NetworkDto networkDto = getNetworkDto(azureParams, null, null, "0.0.0.0/0", 1);
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNetworkHasOneSubnetOnAws() {
        NetworkDto networkDto = getNetworkDto(null, null, null, null, 1);
        environment.setCloudPlatform(CloudPlatform.AWS.name());

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, getSubnetMetas(1));
        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.equals("Cannot create environment, there should be at least two subnets in the network")));
        assertTrue(actual.stream().anyMatch(item ->
                item.equals("Cannot create environment, the subnets in the vpc should be present at least in two different availability zones")));
    }

    @Test
    void testValidateWhenNetworkHasTwoSubnetOnAws() {
        NetworkDto networkDto = getNetworkDto(null, null, null, null, 2);
        environment.setCloudPlatform(CloudPlatform.AWS.name());

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, getSubnetMetas(2));
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNetworkHasTwoSubnetSubnetMetasHasThreeSubnetsOnAws() {
        NetworkDto networkDto = getNetworkDto(null, null, null, null, 2);

        environment.setCloudPlatform(CloudPlatform.AWS.name());

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, getSubnetMetas(3));
        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals("Subnets of the environment () are not found in the vpc (). ", actual);
    }

    @Test
    void testValidateWhenNetworkHasTwoSubnetsWithSameAvailabilityZoneOnAws() {
        NetworkDto networkDto = getNetworkDto(null, null, null, null, 2);

        environment.setCloudPlatform(CloudPlatform.AWS.name());

        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            subnetMetas.put("key" + i, getCloudSubnet("eu-west-1-a"));
        }
        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, subnetMetas);
        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals("Cannot create environment, the subnets in the vpc should be present at least in two different availability zones", actual);
    }

    private NetworkDto getNetworkDto(AzureParams azureParams, AwsParams awsParams, String networkId, String networkCidr, int numberOfSubnets) {
        return NetworkDto.builder()
                .withId(1L)
                .withName("networkName")
                .withResourceCrn("aResourceCRN")
                .withAzure(azureParams)
                .withAws(awsParams)
                .withNetworkCidr(networkCidr)
                .withNetworkId(networkId)
                .withSubnetMetas(getSubnetMetas(numberOfSubnets))
                .build();
    }

    private AzureParams getAzureParams(boolean noPublicIp) {
        return AzureParams.AzureParamsBuilder
                .anAzureParams()
                .withNetworkId("aNetworkId")
                .withResourceGroupName("aResourceGroupId")
                .withNoPublicIp(noPublicIp)
                .build();
    }

    private CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet("eu-west-1", "name", availabilityZone, "cidr");
    }

    private Map<String, CloudSubnet> getSubnetMetas(int numberOfSubnets) {
        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        for (int i = 0; i < numberOfSubnets; i++) {
            subnetMetas.put("key" + i, getCloudSubnet(
                    "eu-west-" + i + "a"));
        }
        return subnetMetas;
    }

}
