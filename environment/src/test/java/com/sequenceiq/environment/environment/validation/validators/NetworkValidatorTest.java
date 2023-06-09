package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.service.EnvironmentTestData;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.environment.validation.network.NetworkTestUtils;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

public class NetworkValidatorTest {

    @Mock
    private EnvironmentNetworkValidator environmentNetworkValidatorMock;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @Mock
    private SubnetUsageValidator subnetUsageValidator;

    private final Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform = new EnumMap<>(CloudPlatform.class);

    private NetworkValidator underTest;

    private Environment environment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new NetworkValidator(environmentNetworkValidatorsByCloudPlatform, environmentDtoConverter, subnetUsageValidator);
        environment = EnvironmentTestData.newTestEnvironment();
        environmentNetworkValidatorsByCloudPlatform.put(CloudPlatform.AZURE, environmentNetworkValidatorMock);
    }

    @Test
    void testValidateWhenAzureSpecificValidationIsCalled() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        environment.setCloudPlatform("aZuRe");
        environment.setCidr(null);

        ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
        verify(environmentNetworkValidatorMock).validateDuringRequest(networkDto, resultBuilder);
    }

    @Test
    void testValidateWhenNetworkCidrAndNetworkIdOnAzure() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), "0.0.0.0/0", 1);
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto);
        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals("The AZURE network id ('aNetworkId') must not be defined if cidr ('0.0.0.0/0') is defined!", actual);
    }

    @Test
    void testValidateWhenNoNetworkCidrAndNetworkIdOnAzure() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNetworkCidrAndNoNetworkIdOnAzure() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, false, false);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNoNetworkCidrAndNoNetworkIdOnYarn() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, YarnParams.builder().build(), null, null, 1);
        environment.setCloudPlatform(CloudPlatform.YARN.name());
        environment.setCidr(null);

        ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNewNetworkAndEnpointGatewaySubnetProvided() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, AwsParams.builder().build(), null, null, "cidr", 1);
        networkDto.setEndpointGatewaySubnetMetas(Map.of("lbsubnet1", new CloudSubnet()));
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        environment.setCidr("cidr");

        ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto);
        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals("The Endpoint Gateway Subnet IDs must not be defined if CIDR ('cidr') is present!", actual);
    }

}
