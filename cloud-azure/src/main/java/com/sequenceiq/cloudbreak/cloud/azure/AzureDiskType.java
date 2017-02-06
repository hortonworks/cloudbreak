package com.sequenceiq.cloudbreak.cloud.azure;

public enum AzureDiskType {

    LOCALLY_REDUNDANT("Standard_LRS", "l"),
    GEO_REDUNDANT("Standard_GRS", "g"),
    PREMIUM_LOCALLY_REDUNDANT("Premium_LRS", "p");

    private final String value;
    private final String abbreviation;

    AzureDiskType(String value, String abbreviation) {
        this.value = value;
        this.abbreviation = abbreviation;
    }

    public String value() {
        return value;
    }

    public String getAbbreviation() {
        return abbreviation;
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
