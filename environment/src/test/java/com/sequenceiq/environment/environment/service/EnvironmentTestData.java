package com.sequenceiq.environment.environment.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class EnvironmentTestData {
    public static final String ACCOUNT_ID = "accid";

    public static final String USER = "userid";

    public static final String CRN = "crnid";

    public static final String ENVIRONMENT_NAME = "envname";

    private EnvironmentTestData() {
    }

    public static CloudRegions getCloudRegions() {
        List<Region> regions = List.of(Region.region("r1"), Region.region("r2"));
        List<String> displayNames = List.of("region 1", "region 2");
        List<Coordinate> coordinates = List.of(Coordinate.coordinate("1", "2", "Here"),
                Coordinate.coordinate("2", "2", "There"));
        List<List<AvailabilityZone>> availabilityZones = List.of(List.of(AvailabilityZone.availabilityZone("r1z1")),
                List.of(AvailabilityZone.availabilityZone("r2z1")));

        Map regionZones = new Zip().intoMap(regions, availabilityZones);
        Map regionDisplayNames = new Zip().intoMap(regions, displayNames);
        Map regionCoordinates = new Zip().intoMap(regions, coordinates);
        return new CloudRegions(regionZones, regionDisplayNames, regionCoordinates, "r1", true);
    }

    static class Zip<T, S> {
        public Map<T, S> intoMap(List<T> t, List<S> s) {
            return IntStream.range(0, Math.min(t.size(), s.size())).boxed()
                    .collect(Collectors.toMap(t::get, s::get));
        }
    }
}
