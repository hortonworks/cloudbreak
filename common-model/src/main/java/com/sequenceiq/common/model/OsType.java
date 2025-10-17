package com.sequenceiq.common.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum OsType {

    CENTOS7("centos7", "redhat7", "CentOS 7", "CentOS 7", "el7",
            true, false),
    RHEL8("redhat8", "redhat8", "Red Hat Enterprise Linux 8", "RHEL 8", "el8",
            false, true),
    RHEL9("redhat9", "redhat9", "Red Hat Enterprise Linux 9", "RHEL 9", "el9",
            false, true);

    private final String os;

    private final String osType;

    private final String name;

    private final String shortName;

    private final String parcelPostfix;

    private final boolean vhdSupported;

    private final boolean freeipaAdminPasswordRotationSupported;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    OsType(String os, String osType, String name, String shortName, String parcelPostfix,
            boolean vhdSupported, boolean freeipaAdminPasswordRotationSupported) {
        this.os = os;
        this.osType = osType;
        this.name = name;
        this.shortName = shortName;
        this.parcelPostfix = parcelPostfix;
        this.vhdSupported = vhdSupported;
        this.freeipaAdminPasswordRotationSupported = freeipaAdminPasswordRotationSupported;
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

    public Set<OsType> getMajorOsTargets() {
        switch (this) {
            case CENTOS7:
                return Set.of(RHEL8);
            case RHEL8:
                return Set.of(RHEL9);
            case RHEL9:
                return Set.of();
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
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

    public static Optional<OsType> getByOsOptional(String os) {
        return Arrays.stream(values())
                .filter(osType -> osType.os.equalsIgnoreCase(os))
                .findFirst();
    }

    public static Optional<OsType> getByOsTypeOptional(String osType) {
        return Arrays.stream(values())
                .filter(os -> os.osType.equalsIgnoreCase(osType))
                .findFirst();
    }

    public static boolean isRhel(String osType) {
        Optional<OsType> osTypeOptional = getByOsTypeOptional(osType);
        return osTypeOptional.isPresent() ? osTypeOptional.get().getOs().toLowerCase(Locale.ROOT).startsWith("redhat") : false;
    }

    public static boolean vhdIsSupported(String osType) {
        Optional<OsType> osTypeOptional = getByOsTypeOptional(osType);
        return osTypeOptional.isPresent() ? osTypeOptional.get().vhdSupported : false;
    }

    public static boolean freeipaAdminPasswordRotationIsSupported(String osType) {
        Optional<OsType> osTypeOptional = getByOsTypeOptional(osType);
        return osTypeOptional.isPresent() ? osTypeOptional.get().freeipaAdminPasswordRotationSupported : false;
    }

    public static OsType getByOs(String os) {
        return getByOsOptional(os)
                .orElseThrow();
    }

    private static Optional<OsType> getOptionalByOsTypeString(String osType) {
        return Arrays.stream(values())
                .filter(e -> e.osType.equals(osType))
                .findFirst();
    }
}
