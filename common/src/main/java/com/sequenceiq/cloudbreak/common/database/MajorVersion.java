package com.sequenceiq.cloudbreak.common.database;

public enum MajorVersion {

    VERSION_10("10"),
    VERSION_11("11"),
    VERSION_12("12"),
    VERSION_13("13"),
    VERSION_14("14");

    private final String version;

    MajorVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

}
