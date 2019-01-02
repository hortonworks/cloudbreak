package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.RegionV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class PlatformRegionsToRegionV4ResponseConverter extends AbstractConversionServiceAwareConverter<CloudRegions, RegionV4Response> {

    @Override
    public RegionV4Response convert(CloudRegions source) {
        RegionV4Response json = new RegionV4Response();

        Set<String> regions = new HashSet<>();
        for (Region region : source.getCloudRegions().keySet()) {
            regions.add(region.value());
        }

        Map<String, Collection<String>> availabilityZones = new HashMap<>();
        for (Entry<Region, List<AvailabilityZone>> regionListEntry : source.getCloudRegions().entrySet()) {
            Collection<String> azs = new ArrayList<>();
            for (AvailabilityZone availabilityZone : regionListEntry.getValue()) {
                azs.add(availabilityZone.value());
            }
            availabilityZones.put(regionListEntry.getKey().value(), azs);
        }

        Map<String, String> displayNames = new HashMap<>();
        for (Entry<Region, String> regionStringEntry : source.getDisplayNames().entrySet()) {
            displayNames.put(regionStringEntry.getKey().value(), regionStringEntry.getValue());
        }

        Set<String> locations = new HashSet<>();
        for (Entry<Region, Coordinate> coordinateEntry : source.getCoordinates().entrySet()) {
            locations.add(coordinateEntry.getKey().getRegionName());
            displayNames.put(coordinateEntry.getKey().getRegionName(), coordinateEntry.getValue().getDisplayName());
        }

        json.setRegions(regions);
        json.setAvailabilityZones(availabilityZones);
        json.setDefaultRegion(source.getDefaultRegion());
        json.setDisplayNames(displayNames);
        json.setLocations(locations);
        return json;
    }
}
