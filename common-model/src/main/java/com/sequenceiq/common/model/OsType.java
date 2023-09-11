package com.sequenceiq.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

@JsonFormat(shape = Shape.OBJECT)
public enum OsType {

    CENTOS7("centos7", "CentOS 7", "CentOS 7"),
    RHEL8("redhat8", "Red Hat Enterprise Linux 8", "RHEL 8");

    private final String os;

    private final String name;

    private final String shortName;

    OsType(String os, String name, String shortName) {
        this.os = os;
        this.name = name;
        this.shortName = shortName;
    }

    public String getOs() {
        return os;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }
}
