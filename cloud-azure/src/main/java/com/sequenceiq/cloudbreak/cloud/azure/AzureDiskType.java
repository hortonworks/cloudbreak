package com.sequenceiq.cloudbreak.cloud.azure;

public enum AzureDiskType {

    LOCALLY_REDUNDANT("Standard_LRS", "l", "Locally-redundant storage"),
    GEO_REDUNDANT("Standard_GRS", "g", "Geo-redundant storage"),
    PREMIUM_LOCALLY_REDUNDANT("Premium_LRS", "p", "Premium locally-redundant storage");

    private final String value;
    private final String abbreviation;
    private final String displayName;

    AzureDiskType(String value, String abbreviation, String displayName) {
        this.value = value;
        this.abbreviation = abbreviation;
        this.displayName = displayName;
    }

    public String value() {
        return value;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String displayName() {
        return displayName;
    }

    public static AzureDiskType getByValue(String value) {
        switch (value) {
            case "Standard_LRS":
                return LOCALLY_REDUNDANT;
            case "Standard_GRS":
                return GEO_REDUNDANT;
            case "Premium_LRS":
                return PREMIUM_LOCALLY_REDUNDANT;
            default:
                return LOCALLY_REDUNDANT;
        }
    }
}
