package com.sequenceiq.cloudbreak.common.database;

public enum TargetMajorVersion implements MajorVersion {

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
