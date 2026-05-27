package com.sequenceiq.cloudbreak.cloud.model;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.common.model.Architecture.ARM64;
import static com.sequenceiq.common.model.Architecture.X86_64;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CloudRegions {

    private final Map<Region, List<AvailabilityZone>> cloudRegions;

    private final Map<Region, String> displayNames;

    private final Map<Region, Coordinate> coordinates;

    private final Map<Region, DefaultVmTypes> defaultVmtypes;

    private final String defaultRegion;

    private final boolean regionsSupported;

    public CloudRegions(
            Map<Region, List<AvailabilityZone>> cloudRegions,
            Map<Region, String> displayNames,
            Map<Region, Coordinate> coordinates,
            Map<Region, DefaultVmTypes> defaultVmtypes,
            String defaultRegion,
            boolean regionsSupported) {
        this.cloudRegions = cloudRegions;
        this.displayNames = displayNames;
        this.coordinates = coordinates;
        this.defaultVmtypes = defaultVmtypes != null ? defaultVmtypes : new HashMap<>();
        this.defaultRegion = defaultRegion;
        this.regionsSupported = regionsSupported;
    }

    public Map<Region, List<AvailabilityZone>> getCloudRegions() {
        return new HashMap<>(cloudRegions);
    }

    public Map<Region, String> getDisplayNames() {
        return new HashMap<>(displayNames);
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public Map<Region, Coordinate> getCoordinates() {
        return coordinates;
    }

    public Map<Region, DefaultVmTypes> getDefaultVmtypes() {
        return defaultVmtypes;
    }

    public Set<String> getRegionNames() {
        Set<String> collect = coordinates.values()
                .stream()
                .map(Coordinate::getDisplayName)
                .collect(Collectors.toSet());

        collect.addAll(coordinates.values()
                .stream()
                .map(Coordinate::getKey)
                .collect(Collectors.toSet()));

        collect.addAll(cloudRegions.keySet()
                .stream()
                .map(Region::getRegionName)
                .collect(Collectors.toSet()));
        return collect;
    }

    public String locationNames() {
        return coordinates.keySet()
                .stream()
                .map(Region::getRegionName)
                .collect(Collectors.joining(", "));
    }

    public Map<Region, List<String>> getDefaultArmFreeIPAVmtypes() {
        return defaultVmtypes.entrySet().stream()
                .filter(e -> e.getValue().getFreeipaVmType(ARM64) != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFreeipaVmType(ARM64)));
    }

    public List<String> getDefaultArmFreeIPAVmtypesByRegion(String region) {
        DefaultVmTypes vmTypes = defaultVmtypes.get(region(region));
        if (vmTypes != null && vmTypes.getFreeipaVmType(ARM64) != null) {
            return vmTypes.getFreeipaVmType(ARM64);
        }
        return List.of();
    }

    public Map<Region, List<String>> getDefaultX86FreeIPAVmtypes() {
        return defaultVmtypes.entrySet().stream()
                .filter(e -> e.getValue().getFreeipaVmType(X86_64) != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFreeipaVmType(X86_64)));
    }

    public List<String> getDefaultX86FreeIPAVmtypesByRegion(String region) {
        DefaultVmTypes vmTypes = defaultVmtypes.get(region(region));
        if (vmTypes != null && vmTypes.getFreeipaVmType(X86_64) != null) {
            return vmTypes.getFreeipaVmType(X86_64);
        }
        return List.of();
    }

    public boolean areRegionsSupported() {
        return regionsSupported;
    }

    @Override
    public String toString() {
        return "CloudRegions{"
                + "cloudRegions=" + cloudRegions
                + ", displayNames=" + displayNames
                + ", coordinates=" + coordinates
                + ", defaultVmtypes=" + defaultVmtypes
                + ", defaultRegion='" + defaultRegion + '\''
                + ", regionsSupported=" + regionsSupported
                + '}';
    }
}
