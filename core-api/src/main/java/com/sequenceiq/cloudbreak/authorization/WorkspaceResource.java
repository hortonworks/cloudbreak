package com.sequenceiq.cloudbreak.authorization;

public enum WorkspaceResource {
    ALL("All resources", "all"),
    WORKSPACE("Workspace", "workspace"),
    CLUSTER_DEFINITION("Cluster definition", "clusterdefinition"),
    IMAGECATALOG("Image catalog", "imagecatalog"),
    CREDENTIAL("Credential", "credential"),
    RECIPE("Recipe", "recipe"),
    STACK("Stack", "stack"),
    STACK_TEMPLATE("Stack template", "stacktemplate"),
    LDAP("LDAP config", "ldap"),
    RDS("RDS config", "rds"),
    PROXY("Proxy config", "proxy"),
    MPACK("MPACK resource", "mpack"),
    KUBERNETES("Kubernetes config", "kube"),
    STRUCTURED_EVENT("Structured event resource", "structuredevent"),
    FLEXSUBSCRIPTION("Flex subscription", "flexsubscription"),
    NETWORK("Network", "network"),
    TOPOLOGY("Topology", "topology"),
    SECURITY_GROUP("Security group", "securitygroup"),
    CONSTRAINT_TEMPLATE("Constraint template", "constrainttemplate"),
    FILESYSTEM("File system", "filesystem"),
    CLUSTER_TEMPLATE("Cluster template", "clustertemplate"),
    ENVIRONMENT("Environment", "env"),
    GATEWAY("Gateway", "gateway"),
    KERBEROS_CONFIG("Kerberos Config", "krbconf"),
    GENERATED_RECIPE("Generated recipe", "generatedrecipe"),
    SECURITY_CONFIG("Security Config", "securityconfig"),
    SALT_SECURITY_CONFIG("Salt Security Config", "saltsecurityconfig"),
    DATALAKE_RESOURCES("Datalake resources", "datalakeresources"),
    SERVICE_DESCRIPTOR("Service Descriptor", "servicedescriptor");

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
