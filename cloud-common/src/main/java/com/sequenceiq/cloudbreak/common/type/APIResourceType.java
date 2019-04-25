package com.sequenceiq.cloudbreak.common.type;

public enum APIResourceType {
    TEMPLATE("t"),
    CONSTRAINT_TEMPLATE("ct"),
    STACK("st"),
    BLUEPRINT("cd"),
    CLUSTER("cl"),
    CREDENTIAL("c"),
    RECIPE("hrec"),
    NETWORK("n"),
    TOPOLOGY("tp"),
    FILESYSTEM("fs"),
    SECURITY_GROUP("sg"),
    CLUSTER_TEMPLATE("ct"),
    RDS_CONFIG("rds"),
    LDAP_CONFIG("ldap"),
    IMAGE_CATALOG("ic"),
    MANAGEMENT_PACK("mpack"),
    WORKSPACE("org");

    private final String namePrefix;

    APIResourceType(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String namePrefix() {
        return namePrefix;
    }
}
