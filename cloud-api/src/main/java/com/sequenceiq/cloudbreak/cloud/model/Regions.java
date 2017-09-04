package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

/**
 * Regions of a platform
 *
 * @see CloudTypes
 * @see Region
 */
public class Regions extends CloudTypes<Region> {

    private Map<Region, DisplayName> displayNames = new HashMap<>();

    public Regions(Collection<Region> types, Region defaultType, Map<Region, DisplayName> displayNames) {
        super(types, defaultType);
        this.displayNames = displayNames;
    }

    public Map<Region, DisplayName> displayNames() {
        return displayNames;
    }
}
