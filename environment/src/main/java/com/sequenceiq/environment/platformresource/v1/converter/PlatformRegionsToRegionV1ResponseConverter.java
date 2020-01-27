package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;

@Component
public class PlatformRegionsToRegionV1ResponseConverter extends AbstractConversionServiceAwareConverter<CloudRegions, RegionResponse> {

    @Override
    public RegionResponse convert(CloudRegions source) {
        RegionResponse json = new RegionResponse();

        List<String> regions = new ArrayList<>();
        for (Region region : source.getCloudRegions().keySet()) {
            regions.add(region.value());
        }

        Map<String, List<String>> availabilityZones = new HashMap<>();
        for (Entry<Region, List<AvailabilityZone>> regionListEntry : source.getCloudRegions().entrySet()) {
            List<String> azs = new ArrayList<>();
            for (AvailabilityZone availabilityZone : regionListEntry.getValue()) {
                azs.add(availabilityZone.value());
            }
            availabilityZones.put(regionListEntry.getKey().value(), azs);
        }

        Map<String, String> displayNames = new HashMap<>();
        for (Entry<Region, String> regionStringEntry : source.getDisplayNames().entrySet()) {
            displayNames.put(regionStringEntry.getKey().value(), regionStringEntry.getValue());
        }

        List<String> locations = new ArrayList<>();
        List<String> k8sSupportedLocations = new ArrayList<>();
        for (Entry<Region, Coordinate> coordinateEntry : source.getCoordinates().entrySet()) {
            locations.add(coordinateEntry.getKey().getRegionName());
            displayNames.put(coordinateEntry.getKey().getRegionName(), coordinateEntry.getValue().getDisplayName());
            if (coordinateEntry.getValue().getK8sSupported()) {
                k8sSupportedLocations.add(coordinateEntry.getKey().getRegionName());
            }
        }

        Collections.sort(regions);
        Collections.sort(locations);
        Collections.sort(k8sSupportedLocations);

        availabilityZones = availabilityZones
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(o -> o.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        displayNames = displayNames
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(o -> o.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        json.setNames(regions);
        json.setAvailabilityZones(availabilityZones);
        json.setDefaultRegion(source.getDefaultRegion());
        json.setDisplayNames(displayNames);
        json.setLocations(locations);
        json.setK8sSupportedlocations(k8sSupportedLocations);

        return json;
    }
}
