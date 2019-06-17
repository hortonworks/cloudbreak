package com.sequenceiq.cloudbreak.common.type;

public enum CloudbreakResourceType {

    NETWORK("network_resource", "network"),
    TEMPLATE("template_resource", "template"),
    INSTANCE("instance_resource", "instance"),
    SECURITY("securitygroup_resource", "securitygroup"),
    IP("ipaddress_resource", "ipaddress"),
    DISK("disk_resource", "disk"),
    STORAGE("storage_resource", "storage"),
    DATABASE("database_resource", "database");

    private final String key;
    private final String templateVariable;

    CloudbreakResourceType(String templateVariable, String key) {
        this.key = key;
        this.templateVariable = templateVariable;
    }

    public String key() {
        return key;
    }

    public String templateVariable() {
        return templateVariable;
    }

}
