package com.sequenceiq.cloudbreak.common.database;

/**
 * Represents the RDS major versions that are available as upgrade targets in CDP.
 */
public enum TargetMajorVersion implements Version {

    VERSION_11("11");

    private final String version;

    TargetMajorVersion(String version) {
        this.version = version;
    }

    @Override
    public String getMajorVersion() {
        return version;
    }

}
