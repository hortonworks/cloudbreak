package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

public class PlatformDatabaseCapabilities {

    private final Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions;

    public PlatformDatabaseCapabilities(Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions) {
        this.enabledRegions = enabledRegions;
    }

    public Map<DatabaseAvailabiltyType, Collection<Region>> getEnabledRegions() {
        return enabledRegions;
    }

}
