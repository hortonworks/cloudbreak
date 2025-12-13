package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.CLOUD_PLATFORM;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.DESCRIPTION;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.LATITUDE;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.LOCATION;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.LOCATION_DISPLAY_NAME;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.LONGITUDE;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.NETWORK_ID;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.REGIONS;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.newTestEnvironment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;

@ExtendWith(MockitoExtension.class)
class EnvironmentViewConverterTest {

    private final CredentialViewConverter credentialViewConverter = mock(CredentialViewConverter.class);

    private final EnvironmentViewConverter underTest = new EnvironmentViewConverter(credentialViewConverter);

    @Test
    void convertTest() {
        Environment environment = newTestEnvironment();

        EnvironmentView result = underTest.convert(environment);

        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(false, result.isArchived());
        assertEquals(CLOUD_PLATFORM, result.getCloudPlatform());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(1L, result.getId());
        assertEquals(LATITUDE, result.getLatitude());
        assertEquals(LOCATION, result.getLocation());
        assertEquals(LOCATION_DISPLAY_NAME, result.getLocationDisplayName());
        assertEquals(LONGITUDE, result.getLongitude());
        assertEquals(ENVIRONMENT_NAME, result.getName());
        assertEquals(NETWORK_ID, result.getNetwork().getNetworkId());
        assertEquals(REGIONS, result.getRegions());
        assertEquals(CRN, result.getResourceCrn());
        assertEquals(EnvironmentStatus.AVAILABLE, result.getStatus());
        assertEquals(environment.getFreeIpaInstanceCountByGroup(), result.getFreeIpaInstanceCountByGroup());
    }
}
