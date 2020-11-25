package com.sequenceiq.cloudbreak.cloud.model.catalog;

public enum ImagePackageVersion {

    CDH_BUILD_NUMBER("cdh-build-number"),
    CFM("cfm"),
    CM("cm"),
    CM_BUILD_NUMBER("cm-build-number"),
    CSA("csa"),
    PROFILER("profiler"),
    SALT("salt"),
    SALT_BOOTSTRAP("salt-bootstrap"),
    SPARK3("spark3"),
    STACK("stack");

    private final String key;

    ImagePackageVersion(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
