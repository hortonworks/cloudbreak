package com.sequenceiq.cloudbreak.domain;

public enum AzureLocation {
    EAST_ASIA("East Asia"),
    SOUTHEAST_ASIA("Southeast Asia"),
    NORTH_EUROPE("North Europe"),
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

}
