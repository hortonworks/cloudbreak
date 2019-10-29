package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.environment.validation.securitygroup.EnvironmentSecurityGroupValidator;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

class EnvironmentNetworkProviderValidatorTest {

    private static final CloudPlatform CLOUD_PLATFORM = CloudPlatform.AWS;

    @Mock
    private EnvironmentRegionValidator environmentRegionValidator;

    @Mock
    private EnvironmentNetworkValidator networkValidator;

    @Mock
    private EnvironmentSecurityGroupValidator securityGroupValidator;

    @Mock
    private Environment environment;

    @Mock
    private CloudRegions cloudRegions;

    private EnvironmentNetworkProviderValidator underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM.name());
        underTest = new EnvironmentNetworkProviderValidator(
                Map.of(CLOUD_PLATFORM, networkValidator),
                Map.of(CLOUD_PLATFORM, securityGroupValidator));
    }

    @Test
    void testIfCloudPlatformMatchesWithTheGivenNetworkInstanceThenItShouldBeFine() {
        NetworkDto network = NetworkDto.builder().withAws(new AwsParams()).build();

        ValidationResult result = underTest.validate(getEnvDto(network));

        assertFalse(result.hasError());
    }

    @Test
    void testIfCloudPlatformDoesNotMatchWithTheGivenNetworkParamInstanceThenErrorComes() {
        NetworkDto network = NetworkDto.builder().withAzure(new AzureParams()).build();

        ValidationResult result = underTest.validate(getEnvDto(network));

        assertTrue(result.hasError());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("The related network parameter for the cloud platform \"" + CLOUD_PLATFORM.name() + "\" has not given!"));
    }

    @Test
    void testIfNetworkParamsHasNotSpecifiedThenNoErrorComes() {
        ValidationResult result = underTest.validate(EnvironmentDto.builder().build());

        assertFalse(result.hasError());
    }

    private EnvironmentDto getEnvDto(NetworkDto network) {
        return EnvironmentDto.builder().withCloudPlatform("AWS").withNetwork(network).build();
    }

}