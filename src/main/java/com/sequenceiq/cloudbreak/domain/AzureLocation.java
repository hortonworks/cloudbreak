package com.sequenceiq.cloudbreak.domain;

public enum AzureLocation {
    EAST_ASIA("East Asia"),
    SOUTHEAST_ASIA("Southeast Asia"),
    NORTH_EUROPE("North Europe"),
    WEST_EUROPE("West Europe"),
    EAST_US("East US"),
    WEST_US("West US"),
    JAPAN_EAST("Japan East"),
    JAPAN_WEST("Japan West"),
    BRAZIL_SOUTH("Brazil South");

    private final String location;

    private AzureLocation(String location) {
        this.location = location;
    }

    public String location() {
        return this.location;
    }

    public static AzureLocation fromName(String regionName) {
        for (AzureLocation region : AzureLocation.values()) {
            if (regionName.equals(region.location())) {
                return region;
            }
        }
        throw new IllegalArgumentException("Cannot create enum from " + regionName + " value!");
    }

}
