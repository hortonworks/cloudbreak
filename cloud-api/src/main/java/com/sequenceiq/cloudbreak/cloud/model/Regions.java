package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

/**
 * Regions of a platform
 *
 * @see CloudTypes
 * @see Region
 */
public class Regions extends CloudTypes<Region> {

    private final Map<Region, DisplayName> displayNames;

    public Regions(Collection<Region> types, Region defaultType, Map<Region, DisplayName> displayNames) {
        super(types, defaultType);
        this.displayNames = displayNames;
    }

    public Map<Region, DisplayName> displayNames() {
        return displayNames;
    }
}
