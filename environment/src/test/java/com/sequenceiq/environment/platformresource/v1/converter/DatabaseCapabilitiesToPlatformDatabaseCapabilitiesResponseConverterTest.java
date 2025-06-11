package com.sequenceiq.environment.platformresource.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseAvailabiltyType;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

class DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverterTest {

    private DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter converter
            = new DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter();

    @Test
    public void testConvert() {
        Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions = new HashMap<>();
        enabledRegions.put(DatabaseAvailabiltyType.databaseAvailabiltyType("big"), new ArrayList<>());
        enabledRegions.put(DatabaseAvailabiltyType.databaseAvailabiltyType("small"), new ArrayList<>());
        Map<Region, String> regionDefaultInstanceTypeMap = new HashMap<>();
        regionDefaultInstanceTypeMap.put(region("region1"), "big");
        regionDefaultInstanceTypeMap.put(region("region2"), "big");
        regionDefaultInstanceTypeMap.put(region("region3"), "big");
        PlatformDatabaseCapabilities source = new PlatformDatabaseCapabilities(
                enabledRegions, regionDefaultInstanceTypeMap,
                new HashMap<>());
        PlatformDatabaseCapabilitiesResponse response = converter.convert(source);


        assertThat(source.getEnabledRegions().size()).isEqualTo(response.getIncludedRegions().size());
        assertThat(source.getRegionDefaultInstanceTypeMap().size()).isEqualTo(response.getRegionDefaultInstances().size());
    }

}