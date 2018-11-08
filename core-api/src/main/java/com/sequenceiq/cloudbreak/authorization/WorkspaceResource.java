package com.sequenceiq.cloudbreak.authorization;

public enum WorkspaceResource {
    ALL("All resources", "all"),
    WORKSPACE("Workspace", "workspace"),
    BLUEPRINT("Blueprint", "blueprint"),
    IMAGECATALOG("Image catalog", "imagecatalog"),
    CREDENTIAL("Credential", "credential"),
    RECIPE("Recipe", "recipe"),
    STACK("Stack", "stack"),
    LDAP("LDAP config", "ldap"),
    RDS("RDS config", "rds"),
    PROXY("Proxy config", "proxy"),
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
