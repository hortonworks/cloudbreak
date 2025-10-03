package com.sequenceiq.common.model;

import java.util.Arrays;
import java.util.Optional;

public enum OsType {

    CENTOS7("centos7", "redhat7", "CentOS 7", "CentOS 7", "el7"),
    RHEL8("redhat8", "redhat8", "Red Hat Enterprise Linux 8", "RHEL 8", "el8"),
    RHEL9("redhat9", "redhat9", "Red Hat Enterprise Linux 9", "RHEL 9", "el9");

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

    public boolean matches(String os, String osType) {
        return this.os.equalsIgnoreCase(os) && this.osType.equalsIgnoreCase(osType);
    }

    public static OsType getByOsTypeString(String osType) {
        return getOptionalByOsTypeString(osType).orElse(null);
    }

    public static OsType getByOsTypeStringWithCentos7Fallback(String osType) {
        return getOptionalByOsTypeString(osType).orElse(CENTOS7);
    }

    public static OsType getByOs(String os) {
        return Arrays.stream(values()).filter(osType -> osType.os.equalsIgnoreCase(os)).findFirst().orElseThrow();
    }

    private static Optional<OsType> getOptionalByOsTypeString(String osType) {
        return Arrays.stream(values())
                .filter(e -> e.osType.equals(osType))
                .findFirst();
    }
}
