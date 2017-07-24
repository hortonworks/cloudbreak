package com.sequenceiq.cloudbreak.common.type;

public enum APIResourceType {
    TEMPLATE("t"),
    CONSTRAINT_TEMPLATE("ct"),
    STACK("st"),
    BLUEPRINT("bp"),
    CLUSTER("cl"),
    CREDENTIAL("c"),
    RECIPE("hrec"),
    SSSDCONFIG("sssd"),
    NETWORK("n"),
    TOPOLOGY("tp"),
    SECURITY_GROUP("sg"),
    CLUSTER_TEMPLATE("ct"),
    RDS_CONFIG("rds"),
    LDAP_CONFIG("ldap"),
    SMARTSENSE_SUBSCRIPTION("sss"),
    FLEX_SUBSCRIPTION("fs");

    private final String namePrefix;

    APIResourceType(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String namePrefix() {
        return namePrefix;
    }
}
