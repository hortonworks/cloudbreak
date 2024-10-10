package com.sequenceiq.environment.platformresource.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProviderResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProvidersResponse;

class PlatformRegionsToExpressOnboardingRegionsResponseConverterTest {

    private PlatformRegionsToExpressOnboardingRegionsResponseConverter underTest
            = new PlatformRegionsToExpressOnboardingRegionsResponseConverter();

    @Test
    public void testConvert() {
        CloudRegions source = mock(CloudRegions.class);

        Region region1 = mock(Region.class);
        Region region2 = mock(Region.class);
        when(region1.getRegionName()).thenReturn("Region1");
        when(region2.getRegionName()).thenReturn("Region2");

        Map<Region, String> displayNames = new HashMap<>();
        displayNames.put(region1, "DisplayName1");
        displayNames.put(region2, "DisplayName2");
        when(source.getDisplayNames()).thenReturn(displayNames);

        Coordinate coord1 = mock(Coordinate.class);
        Coordinate coord2 = mock(Coordinate.class);
        when(coord1.getDisplayName()).thenReturn("CoordDisplay1");
        when(coord1.getKey()).thenReturn("CoordKey1");
        when(coord2.getDisplayName()).thenReturn("CoordDisplay2");
        when(coord2.getKey()).thenReturn("CoordKey2");

        when(coord1.getCdpSupportedServices()).thenReturn(Set.of(CdpSupportedServices.CDE, CdpSupportedServices.CDF));
        when(coord2.getCdpSupportedServices()).thenReturn(Set.of(CdpSupportedServices.CDE));

        Map<Region, Coordinate> coordinates = new HashMap<>();
        coordinates.put(region1, coord1);
        coordinates.put(region2, coord2);
        when(source.getCoordinates()).thenReturn(coordinates);

        ExpressOnboardingCloudProvidersResponse response = underTest.convert(source);

        assertEquals(2, response.getRegions().size());

        Optional<ExpressOnboardingCloudProviderResponse> regionResponse1 = response.getRegions()
                .stream()
                .filter(e -> e.getName().equals("CoordKey1"))
                .findFirst();
        assertTrue(!regionResponse1.isEmpty());
        assertEquals("CoordDisplay1", regionResponse1.get().getLabel());
        assertEquals("CoordKey1", regionResponse1.get().getName());
        assertTrue(regionResponse1.get().getServices().containsAll(Set.of("cde", "cdf")));

        Optional<ExpressOnboardingCloudProviderResponse> regionResponse2 = response.getRegions()
                .stream()
                .filter(e -> e.getName().equals("CoordKey2"))
                .findFirst();
        assertTrue(!regionResponse2.isEmpty());
        assertEquals("CoordDisplay2", regionResponse2.get().getLabel());
        assertEquals("CoordKey2", regionResponse2.get().getName());
        assertEquals(Collections.singletonList("cde"), regionResponse2.get().getServices());
    }
}