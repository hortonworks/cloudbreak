package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseAvailabiltyType;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

@Component
public class DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter {

    public PlatformDatabaseCapabilitiesResponse convert(PlatformDatabaseCapabilities source) {
        Map<String, List<String>> includedRegions = new HashMap<>();
        for (Map.Entry<DatabaseAvailabiltyType, Collection<Region>> databaseEntry : source.getEnabledRegions().entrySet()) {
            String databaseAvailabilityType = databaseEntry.getKey().getValue();
            if (!includedRegions.containsKey(databaseAvailabilityType)) {
                includedRegions.put(databaseAvailabilityType, new ArrayList<>());
            }

            includedRegions.get(databaseAvailabilityType).addAll(
                    databaseEntry.getValue()
                        .stream()
                        .map(e -> e.getRegionName())
                        .collect(Collectors.toList()));
        }
        Map<String, String> defaultTypes = new HashMap<>();
        for (Map.Entry<Region, String> typeEntry : source.getRegionDefaultInstanceTypeMap().entrySet()) {
            String region = typeEntry.getKey().getValue();
            defaultTypes.put(region, typeEntry.getValue());
        }
        Map<String, Map<String, List<String>>> regionUpgradeVersions = new HashMap<>();
        for (Map.Entry<Region, Map<String, List<String>>> upgradeEntry : source.getSupportedServerVersionsToUpgrade().entrySet()) {
            String region = upgradeEntry.getKey().getRegionName();
            regionUpgradeVersions.put(region, upgradeEntry.getValue());
        }
        return new PlatformDatabaseCapabilitiesResponse(includedRegions, defaultTypes, regionUpgradeVersions);
    }
}
