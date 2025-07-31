package com.sequenceiq.cloudbreak.common.database;

/**
 * Represents the RDS major versions that are available as upgrade targets in CDP.
 */
public enum TargetMajorVersion implements Version {

    VERSION11("11"),
    VERSION14("14"),
    VERSION17("17"),
    @Deprecated
    VERSION_11("11"),
    @Deprecated
    VERSION_14("14");

    private final String version;

    TargetMajorVersion(String version) {
        this.version = version;
    }

    @Override
    public String getMajorVersion() {
        return version;
    }

    public MajorVersion convertToMajorVersion() {
        return switch (this) {
            case VERSION_11 -> MajorVersion.VERSION_11;
            case VERSION_14 -> MajorVersion.VERSION_14;
            case VERSION11 -> MajorVersion.VERSION_11;
            case VERSION14 -> MajorVersion.VERSION_14;
            case VERSION17 -> MajorVersion.VERSION_17;
        };
    }

    public static TargetMajorVersion fromVersion(String version) {
        if (version == null) {
            return null;
        }

        for (TargetMajorVersion targetVersion : TargetMajorVersion.values()) {
            if (targetVersion.getMajorVersion().equals(version)) {
                return targetVersion;
            }
        }

        return null;
    }
}
