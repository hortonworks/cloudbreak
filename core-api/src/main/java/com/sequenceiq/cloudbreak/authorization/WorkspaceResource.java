package com.sequenceiq.cloudbreak.authorization;

public enum WorkspaceResource {
    ALL("All resources", "all"),
    WORKSPACE("Workspaces", "workspace"),
    BLUEPRINT("Blueprints", "blueprint"),
    IMAGECATALOG("Image catalogs", "imagecatalog"),
    CREDENTIAL("Credentials", "credential"),
    RECIPE("Recipes", "recipe"),
    STACK("Stacks", "stack"),
    LDAP("LDAP resource", "ldap"),
    RDS("RDS resource", "rds"),
    PROXY("Proxys", "proxy"),
    MPACK("MPACK resource", "mpack"),
    STRUCTURED_EVENT("Structured event resource", "structuredevent"),
    FLEXSUBSCRIPTION("Flex subscription", "flexsubscription"),
    NETWORK("Network", "network"),
    TOPOLOGY("Topology", "topology"),
    SECURITY_GROUP("Security group", "securitygroup"),
    CONSTRAINT_TEMPLATE("Constraint template", "constrainttemplate"),
    FILESYSTEM("File system", "filesystem"),
    CLUSTER_TEMPLATE("Cluster template", "clustertemplate"),
    ENVIRONMENT("Environment", "env");

    private final String readableName;

    private final String shortName;

    WorkspaceResource(String readableName, String shortName) {
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
