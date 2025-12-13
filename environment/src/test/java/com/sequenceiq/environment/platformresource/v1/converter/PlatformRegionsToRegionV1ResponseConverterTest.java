package com.sequenceiq.environment.platformresource.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;

@ExtendWith(MockitoExtension.class)
class PlatformRegionsToRegionV1ResponseConverterTest {

    @InjectMocks
    private PlatformRegionsToRegionV1ResponseConverter regionResponseConverter;

    private CloudRegions mockCloudRegions;

    @BeforeEach
    public void setUp() {
        Region region1 = region("region-1");
        Region region2 = region("region-2");

        AvailabilityZone az1 = new AvailabilityZone("az-1");
        AvailabilityZone az2 = new AvailabilityZone("az-2");

        Map<Region, List<AvailabilityZone>> cloudRegions = new HashMap<>();
        cloudRegions.put(region1, Arrays.asList(az1));
        cloudRegions.put(region2, Arrays.asList(az2));

        Map<Region, String> displayNames = new HashMap<>();
        displayNames.put(region1, "Region 1 Display Name");
        displayNames.put(region2, "Region 2 Display Name");

        Map<Region, Coordinate> coordinates = new HashMap<>();
        Coordinate coord1 = coordinate(
                "1",
                "2",
                "coord1",
                "coord1",
                true,
                List.of(),
                "deafult",
                "defaultArm",
                new HashSet<>(Arrays.asList(CdpSupportedServices.ALL)));
        Coordinate coord2 = coordinate(
                "1",
                "2",
                "coord2",
                "coord2",
                true,
                List.of(),
                "deafult",
                "defaultArm",
                new HashSet<>(Arrays.asList(CdpSupportedServices.ALL)));
        coordinates.put(region1, coord1);
        coordinates.put(region2, coord2);

        mockCloudRegions = new CloudRegions(
            cloudRegions,
            displayNames,
            coordinates,
            "defaultRegion",
            true
        );
    }

    @Test
    public void testConvertSuccess() {
        RegionResponse response = regionResponseConverter.convert(mockCloudRegions);

        assertEquals(Arrays.asList("region-1", "region-2"), response.getNames());

        Map<String, List<String>> expectedAvailabilityZones = new HashMap<>();
        expectedAvailabilityZones.put("region-1", Collections.singletonList("az-1"));
        expectedAvailabilityZones.put("region-2", Collections.singletonList("az-2"));
        assertEquals(expectedAvailabilityZones, response.getAvailabilityZones());

        Map<String, String> expectedDisplayNames = new LinkedHashMap<>();
        expectedDisplayNames.put("region-1", "coord1");
        expectedDisplayNames.put("region-2", "coord2");
        assertEquals(expectedDisplayNames, response.getDisplayNames());

        assertEquals(Arrays.asList("region-1", "region-2"), response.getLocations());

        assertEquals(List.of("region-1", "region-2"), response.getK8sSupportedlocations());

        Map<String, Set<String>> expectedServices = new HashMap<>();
        expectedServices.put("region-1", new HashSet<>(Collections.singletonList("all")));
        expectedServices.put("region-2", new HashSet<>(Collections.singletonList("all")));
        assertEquals(expectedServices, response.getCdpSupportedServices());

        assertEquals("defaultRegion", response.getDefaultRegion());
    }

}