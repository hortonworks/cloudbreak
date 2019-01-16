package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentEditRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.LocationRequest;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

public class EnvironmentUtils {

    private EnvironmentUtils() {
    }

    public static CloudRegions getCloudRegions(Set<String> regions) {
        Map<Region, List<AvailabilityZone>> cloudRegions = regions.stream().collect(Collectors.toMap(Region::region, r -> Collections.emptyList()));
        Map<Region, String> displayNames = regions.stream().collect(Collectors.toMap(Region::region, r -> r));
        Map<Region, Coordinate> coordinates = regions.stream().collect(Collectors.toMap(Region::region, r -> coordinate("0", "1", r)));
        return new CloudRegions(cloudRegions, displayNames, coordinates, regions.iterator().next(), true);
    }

    public static CloudRegions getCloudRegions() {
        return getCloudRegions(true);
    }

    public static CloudRegions getCloudRegions(boolean regionsSupported) {
        return new CloudRegions(
                Map.of(region("region1"), Collections.emptyList(), region("region2"), Collections.emptyList()),
                Map.of(region("region1"), "display1", region("region2"), "display2"),
                Map.of(region("region1"), coordinate("1", "2", "region1"),
                        region("region2"), coordinate("1", "2", "region2")),
                "region1",
                regionsSupported);
    }

    public static Environment getEnvironment(String location, Set<String> regions) {
        Environment environment = new Environment();
        environment.setLocation(location);
        environment.setLocationDisplayName(location);
        Set<com.sequenceiq.cloudbreak.domain.environment.Region> regionSet = regions.stream().map(r -> {
            com.sequenceiq.cloudbreak.domain.environment.Region region = new com.sequenceiq.cloudbreak.domain.environment.Region();
            region.setName(r);
            region.setDisplayName(r);
            return region;
        }).collect(Collectors.toSet());
        environment.setRegions(regionSet);
        return environment;
    }

    public static EnvironmentEditRequest getEnvironmentEditRequest(String description, String location, Set<String> regions) {
        EnvironmentEditRequest editRequest = new EnvironmentEditRequest();
        editRequest.setDescription(description);
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName(location);
        locationRequest.setLongitude(1D);
        locationRequest.setLatitude(2D);
        editRequest.setLocation(locationRequest);
        editRequest.setRegions(regions);
        return editRequest;
    }
}
