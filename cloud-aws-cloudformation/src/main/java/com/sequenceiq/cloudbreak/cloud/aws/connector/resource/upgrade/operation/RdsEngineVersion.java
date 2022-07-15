package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class RdsEngineVersion implements Versioned {

    private final String version;

    private final String majorVersion;

    public RdsEngineVersion(String version) {
        this.version = version;
        majorVersion = version.split("\\.")[0];
    }

    @Override
    public String getVersion() {
        return version;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    @Override
    public String toString() {
        return "RdsEngineVersion{" +
                "version='" + version + '\'' +
                ", major='" + majorVersion + '\'' +
                '}';
    }
}