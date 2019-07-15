package com.sequenceiq.environment.environment.v1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.environment.domain.Region;

@Component
public class RegionConverter {
    public CompactRegionResponse convertRegions(Set<Region> regions) {
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        Set<String> values = new HashSet<>();
        Map<String, String> displayNames = new HashMap<>();
        for (Region region : regions) {
            values.add(region.getName());
            displayNames.put(region.getName(), region.getDisplayName());
        }
        compactRegionResponse.setNames(values);
        compactRegionResponse.setDisplayNames(displayNames);
        return compactRegionResponse;
    }
}
