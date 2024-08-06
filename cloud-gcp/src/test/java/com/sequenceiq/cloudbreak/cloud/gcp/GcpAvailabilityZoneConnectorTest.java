package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
public class GcpAvailabilityZoneConnectorTest {

    private static final Region GCP_REGION = Region.region("west-us2");

    private static final String GCP_INSTANCE_TYPE = "GcpInstanceType";

    @Mock
    private GcpPlatformResources gcpPlatformResources;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @InjectMocks
    private GcpAvailabilityZoneConnector gcpAvailabilityZoneConnector;

    @Test
    void testGetAvailabilityZonesWithNoResponseForGivenRegion() {
        when(gcpPlatformResources.getAvailabilityZonesForVmTypes(extendedCloudCredential, GCP_REGION)).thenReturn(Map.of());
        Set<String> availabilityZones = gcpAvailabilityZoneConnector.getAvailabilityZones(extendedCloudCredential,
                Set.of("us-west2-a", "us-west2-b", "us-west2-c"),
                GCP_INSTANCE_TYPE, GCP_REGION);
        assertEquals(Collections.emptySet(), availabilityZones);
    }

    static Object [] [] dataForTestGetAvailabilityZones() {
        return new Object[] [] {
                {Set.of("us-west2-a", "us-west2-b", "us-west2-c"), Collections.emptySet(), Collections.emptySet()},
                {Collections.emptySet(), Set.of("us-west2-a", "us-west2-b", "us-west2-c"), Collections.emptySet()},
                {null, Set.of("us-west2-a", "us-west2-b", "us-west2-c"), Collections.emptySet()},
                {Set.of("us-west2-a", "us-west2-b", "us-west2-c"), Set.of("us-west2-a"), Set.of("us-west2-a")},
                {Set.of("us-west2-a", "us-west2-b"), Set.of("us-west2-b", "us-west2-c"), Set.of("us-west2-b")}
        };
    }

    @ParameterizedTest(name = "testGetAvailabilityZones{index}")
    @MethodSource("dataForTestGetAvailabilityZones")
    void testGetAvailabilityZones(Set<String> environmentZones, Set<String> zonesSupportedForInstanceType, Set<String> expectedZones) {
        when(gcpPlatformResources.getAvailabilityZonesForVmTypes(extendedCloudCredential, GCP_REGION))
                .thenReturn(Map.of(GCP_INSTANCE_TYPE, zonesSupportedForInstanceType));
        Set<String> availabilityZones = gcpAvailabilityZoneConnector.getAvailabilityZones(extendedCloudCredential, environmentZones,
                GCP_INSTANCE_TYPE, GCP_REGION);
        assertEquals(expectedZones, availabilityZones);
    }
}
