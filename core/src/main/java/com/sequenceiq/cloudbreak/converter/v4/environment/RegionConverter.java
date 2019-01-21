package com.sequenceiq.cloudbreak.converter.v4.environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.CompactRegionV4Response;
import com.sequenceiq.cloudbreak.domain.environment.Region;

@Component
public class RegionConverter {
    public CompactRegionV4Response convertRegions(Set<Region> regions) {
        CompactRegionV4Response compactRegionResponse = new CompactRegionV4Response();
        Set<String> values = new HashSet<>();
        Map<String, String> displayNames = new HashMap<>();
        for (Region region : regions) {
            values.add(region.getName());
            displayNames.put(region.getName(), region.getDisplayName());
        }
        compactRegionResponse.setRegions(values);
        compactRegionResponse.setDisplayNames(displayNames);
        return compactRegionResponse;
    }
}
