package com.sequenceiq.cloudbreak.cloud.azure.resource.domain;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Coordinate;

public class AzureCoordinate extends Coordinate {

    private final boolean flexibleSameZoneEnabled;

    private final boolean flexibleZoneRedundantEnabled;

    //CHECKSTYLE:OFF
    private AzureCoordinate(Double longitude, Double latitude, String displayName, String key, boolean k8sSupported, List<String> entitlements,
            boolean flexibleSameZoneEnabled, boolean flexibleZoneRedundantEnabled) {
        super(longitude, latitude, displayName, key, k8sSupported, entitlements);
        this.flexibleSameZoneEnabled = flexibleSameZoneEnabled;
        this.flexibleZoneRedundantEnabled = flexibleZoneRedundantEnabled;
    }
    //CHECKSTYLE:ON

    public boolean isFlexibleSameZoneEnabled() {
        return flexibleSameZoneEnabled;
    }

    public boolean isFlexibleZoneRedundantEnabled() {
        return flexibleZoneRedundantEnabled;
    }

    //CHECKSTYLE:OFF
    public static AzureCoordinate coordinate(String longitude, String latitude, String  displayName, String key, boolean k8sSupported,
            List<String> entitlements, boolean flexibleSameZoneEnabled, boolean flexibleZoneRedundantEnabled) {
        return new AzureCoordinate(Double.parseDouble(longitude), Double.parseDouble(latitude), displayName, key,
                k8sSupported, entitlements, flexibleSameZoneEnabled, flexibleZoneRedundantEnabled);
    }
    //CHECKSTYLE:ON

    public static AzureCoordinate defaultCoordinate() {
        return new AzureCoordinate(
                Double.parseDouble("36.7477169"),
                Double.parseDouble("-119.7729841"),
                "California (West US)",
                "us-west-1",
                false,
                new ArrayList<>(),
                false,
                false);
    }
}
