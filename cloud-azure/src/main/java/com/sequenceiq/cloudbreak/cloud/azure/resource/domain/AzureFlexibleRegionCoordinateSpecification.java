package com.sequenceiq.cloudbreak.cloud.azure.resource.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AzureFlexibleRegionCoordinateSpecification {

    @JsonProperty("sameZoneEnabled")
    private boolean sameZoneEnabled;

    @JsonProperty("zoneRedundantEnabled")
    private boolean zoneRedundantEnabled;

    public boolean isSameZoneEnabled() {
        return sameZoneEnabled;
    }

    public void setSameZoneEnabled(boolean sameZoneEnabled) {
        this.sameZoneEnabled = sameZoneEnabled;
    }

    public boolean isZoneRedundantEnabled() {
        return zoneRedundantEnabled;
    }

    public void setZoneRedundantEnabled(boolean zoneRedundantEnabled) {
        this.zoneRedundantEnabled = zoneRedundantEnabled;
    }
}
