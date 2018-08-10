package com.sequenceiq.cloudbreak.authorization;

public enum OrganizationResource {
    ALL("All resources", "all"),
    ORG("Organizations", "organization"),
    BLUEPRINT("Blueprints", "blueprint"),
    IMAGECATALOG("Image catalogs", "imagecatalog"),
    CREDENTIAL("Credentials", "credential"),
    RECIPE("Recipes", "recipe"),
    STACK("Stacks", "stack"),
    LDAP("LDAP resource", "ldap"),
    RDS("RDS resource", "rds"),
    PROXY("Proxys", "proxy"),
    MPACK("MPACK resource", "mpack");

    private final String readableName;

    private final String shortName;

    OrganizationResource(String readableName, String shortName) {
        this.readableName = readableName;
        this.shortName = shortName;
    }

    public String getReadableName() {
        return readableName;
    }

    public String getShortName() {
        return shortName;
    }
}
