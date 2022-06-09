package com.sequenceiq.cloudbreak.common.database;

public enum TargetMajorVersion {

    VERSION_11("11");

    private final String version;

    TargetMajorVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

}
