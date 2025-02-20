package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.azure.core.management.Region;

public class RegionUtil {

    private static final Map<String, Region> REGIONS_BY_NAME;

    static {
        REGIONS_BY_NAME = Region.values()
                .stream()
                .collect(Collectors.toMap(Region::name, Function.identity()));
    }

    private RegionUtil() {
    }

    public static Region findByLabelOrName(String labelOrName) {
        if (labelOrName == null) {
            return null;
        }
        Region region = REGIONS_BY_NAME.get(labelOrName.toLowerCase(Locale.ROOT).replace(" ", ""));
        if (region == null) {
            region = Region.fromName(labelOrName);
        }
        return region;
    }
}
