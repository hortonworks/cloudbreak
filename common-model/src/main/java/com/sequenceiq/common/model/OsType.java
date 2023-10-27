package com.sequenceiq.common.model;

public enum OsType {

    CENTOS7("centos7", "redhat7", "CentOS 7", "CentOS 7"),
    RHEL8("redhat8", "redhat8", "Red Hat Enterprise Linux 8", "RHEL 8");

    private final String os;

    private final String osType;

    private final String name;

    private final String shortName;

    OsType(String os, String osType, String name, String shortName) {
        this.os = os;
        this.osType = osType;
        this.name = name;
        this.shortName = shortName;
    }

    public String getOs() {
        return os;
    }

    public String getOsType() {
        return osType;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }
}
