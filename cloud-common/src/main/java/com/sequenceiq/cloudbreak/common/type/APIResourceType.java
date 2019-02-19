package com.sequenceiq.cloudbreak.common.type;

public enum APIResourceType {
    TEMPLATE("t"),
    CONSTRAINT_TEMPLATE("ct"),
    STACK("st"),
    CLUSTER_DEFINITION("cd"),
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
    SMARTSENSE_SUBSCRIPTION("sss"),
    IMAGE_CATALOG("ic"),
    FLEX_SUBSCRIPTION("fs"),
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
