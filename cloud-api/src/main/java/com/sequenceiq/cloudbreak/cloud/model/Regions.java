package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

/**
 * Regions of a platform
 *
 * @see CloudTypes
 * @see Region
 */
public class Regions extends CloudTypes<Region> {
    public Regions(Collection<Region> types, Region defaultType) {
        super(types, defaultType);
    }
}
