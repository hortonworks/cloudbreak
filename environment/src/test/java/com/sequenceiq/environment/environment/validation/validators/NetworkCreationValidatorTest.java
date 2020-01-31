package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentTestData;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.environment.validation.network.NetworkTestUtils;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

public class NetworkCreationValidatorTest {

    private EnvironmentNetworkValidator environmentNetworkValidatorMock = mock(EnvironmentNetworkValidator.class);

    private final Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform = new EnumMap<>(CloudPlatform.class);

    private NetworkCreationValidator underTest = new NetworkCreationValidator(environmentNetworkValidatorsByCloudPlatform);

    private Environment environment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        environment = EnvironmentTestData.newTestEnvironment();
        environmentNetworkValidatorsByCloudPlatform.put(CloudPlatform.AZURE, environmentNetworkValidatorMock);
    }

    @Test
    void testValidateWhenAzureSpecificValidationIsCalled() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), null, 1);
        environment.setCloudPlatform("aZuRe");
        environment.setCidr(null);

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
        verify(environmentNetworkValidatorMock).validateDuringRequest(networkDto, null, resultBuilder);
    }

    @Test
    void testValidateWhenNetworkCidrAndNetworkIdOnAzure() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, true, true);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, azureParams.getNetworkId(), "0.0.0.0/0", 1);
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);
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

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNetworkCidrAndNoNetworkIdOnAzure() {
        AzureParams azureParams = NetworkTestUtils.getAzureParams(true, false, false);
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(azureParams, null, null, null, "0.0.0.0/0", 1);
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setCidr(null);

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenNoNetworkCidrAndNoNetworkIdOnYarn() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, new YarnParams(), null, null, 1);
        environment.setCloudPlatform(CloudPlatform.YARN.name());
        environment.setCidr(null);

        ValidationResult.ValidationResultBuilder resultBuilder = underTest.validateNetworkCreation(environment, networkDto, null);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }
}
