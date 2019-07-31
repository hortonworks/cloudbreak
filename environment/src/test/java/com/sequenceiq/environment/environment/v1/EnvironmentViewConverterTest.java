package com.sequenceiq.environment.environment.v1;

import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ACCOUNT_ID;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.CLOUD_PLATFORM;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.CREDENTIAL_CRN;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.CRN;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

class EnvironmentViewConverterTest {

    EnvironmentViewConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnvironmentViewConverter();
    }

    @Test
    void convertTest() {
        Environment environment = newTestEnvironment();

        EnvironmentView result = underTest.convert(environment);

        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(false, result.isArchived());
        assertEquals(CLOUD_PLATFORM, result.getCloudPlatform());
        assertEquals(CREDENTIAL_CRN, result.getCredential().getResourceCrn());
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
    }
}
