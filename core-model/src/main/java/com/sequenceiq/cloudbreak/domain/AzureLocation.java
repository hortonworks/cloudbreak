package com.sequenceiq.cloudbreak.domain;

public enum AzureLocation {
    EAST_ASIA("East Asia"),
    NORTH_EUROPE("North Europe"),
    WEST_EUROPE("West Europe"),
    EAST_US("East US"),
    CENTRAL_US("Central US"),
    SOUTH_CENTRAL_US("South Central US"),
    NORTH_CENTRAL_US("North Central US"),
    EAST_US_2("East US 2"),
    WEST_US("West US"),
    JAPAN_EAST("Japan East"),
    JAPAN_WEST("Japan West"),
    SOUTHEAST_ASIA("Southeast Asia"),
    BRAZIL_SOUTH("Brazil South");

    private final String region;

    private AzureLocation(String region) {
        this.region = region;
    }

    public String region() {
        return this.region;
    }

    public static AzureLocation fromName(String regionName) {
        for (AzureLocation region : AzureLocation.values()) {
            if (regionName.equals(region.region())) {
                return region;
            }
        }
        throw new IllegalArgumentException("Cannot getAmbariClient enum from " + regionName + " value!");
    }

}
