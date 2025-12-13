package com.sequenceiq.redbeams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class DatabaseCapabilityServiceTest {
    @Mock
    private CloudConnector connector;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Mock
    private PlatformResources platformResources;

    @Mock
    private Region region;

    private DatabaseCapabilityService databaseCapabilityService;

    @BeforeEach
    void setUp() {
        databaseCapabilityService = new DatabaseCapabilityService();
    }

    @Test
    void testGetDefaultInstanceType() {
        PlatformDatabaseCapabilities databaseCapabilities = mock(PlatformDatabaseCapabilities.class);
        when(connector.platformResources()).thenReturn(platformResources);
        when(platformResources.databaseCapabilities(cloudCredential, region, Map.of())).thenReturn(databaseCapabilities);

        when(databaseCapabilities.getRegionDefaultInstanceTypeMap()).thenReturn(Collections.singletonMap(region, "Standard_E4ds_v5"));

        String defaultInstanceType = databaseCapabilityService.getDefaultInstanceType(connector, cloudCredential, cloudPlatformVariant, region);

        assertEquals("Standard_E4ds_v5", defaultInstanceType);
    }

    @Test
    void testGetDefaultInstanceTypeWhenInstanceTypesAreEmpty() {
        PlatformDatabaseCapabilities databaseCapabilities = mock(PlatformDatabaseCapabilities.class);
        when(connector.platformResources()).thenReturn(platformResources);
        when(platformResources.databaseCapabilities(cloudCredential, region, Map.of())).thenReturn(databaseCapabilities);

        when(databaseCapabilities.getRegionDefaultInstanceTypeMap()).thenReturn(Map.of());

        String defaultInstanceType = databaseCapabilityService.getDefaultInstanceType(connector, cloudCredential, cloudPlatformVariant, region);

        assertNull(defaultInstanceType);
    }

    @Test
    void testGetDefaultInstanceTypeWhenRuntimeException() {
        PlatformDatabaseCapabilities databaseCapabilities = mock(PlatformDatabaseCapabilities.class);
        when(connector.platformResources()).thenReturn(platformResources);
        when(platformResources.databaseCapabilities(cloudCredential, region, Map.of())).thenThrow(new UnsupportedOperationException("ex"));

        String defaultInstanceType = databaseCapabilityService.getDefaultInstanceType(connector, cloudCredential, cloudPlatformVariant, region);

        assertNull(defaultInstanceType);
    }
}
