package com.sequenceiq.common.model;

import java.util.Arrays;

public enum OsType {

    CENTOS7("centos7", "redhat7", "CentOS 7", "CentOS 7", "el7"),
    RHEL8("redhat8", "redhat8", "Red Hat Enterprise Linux 8", "RHEL 8", "el8");

    private final String os;

    private final String osType;

    private final String name;

    private final String shortName;

    private final String parcelPostfix;

    OsType(String os, String osType, String name, String shortName, String parcelPostfix) {
        this.os = os;
        this.osType = osType;
        this.name = name;
        this.shortName = shortName;
        this.parcelPostfix = parcelPostfix;
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

    public String getParcelPostfix() {
        return parcelPostfix;
    }

    public static OsType getByOsTypeString(String osType) {
        return Arrays.stream(values())
                .filter(e -> e.osType.equals(osType))
                .findFirst()
                .orElse(null);
    }
}